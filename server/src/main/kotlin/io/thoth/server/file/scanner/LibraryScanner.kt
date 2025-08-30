package io.thoth.server.file.scanner

import io.thoth.models.Library
import io.thoth.server.common.extensions.findOne
import io.thoth.server.database.access.hasBeenUpdated
import io.thoth.server.database.access.markAsTouched
import io.thoth.server.database.access.toModel
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookEntity
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageTable
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TrackEntity
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.file.TrackManager
import io.thoth.server.repositories.LibraryRepository
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging.logger
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString
import kotlin.io.path.getLastModifiedTime

interface LibraryScanner {
    fun fullScan(libraryIDs: List<UUID>)

    fun scanLibrary(library: Library)

    fun scanLibrary(library: Library)

    fun scanFolder(
        folder: Path,
        library: Library?,
    )

    fun unIgnoreFolder(folder: Path)

    fun ignoreFolder(folder: Path)

    fun removePath(path: Path)

    fun isIgnored(folder: Path): Boolean

    suspend fun addOrUpdatePath(
        path: Path,
        library: Library?,
    )
}

class LibraryScannerImpl :
    LibraryScanner,
    KoinComponent {
    private val trackManager: TrackManager by inject()
    private val libraryRepository: LibraryRepository by inject()

    companion object {
        private val fullScanIsOngoing = AtomicBoolean()
        private val foldersToIgnore = mutableSetOf<Path>()
        private val log = logger {}
    }

    override fun isIgnored(folder: Path): Boolean = foldersToIgnore.any { it.startsWith(folder) }

    override fun unIgnoreFolder(folder: Path) {
        foldersToIgnore.removeIf { it.absolutePathString() == folder.absolutePathString() }
    }

    override fun ignoreFolder(folder: Path) {
        if (!foldersToIgnore.any { it.absolutePathString() == folder.absolutePathString() }) {
            return
        }
        foldersToIgnore.add(folder)
        removePath(folder)
    }

    override fun removePath(path: Path) {
        trackManager.removePath(path)
        val library =
            transaction { libraryRepository.getMatching(path) } ?: return log.warn { "No library found for path $path" }
        cleanupLibrary(library)
    }

    override fun fullScan(libraryIDs: List<UUID>) {
        log.info { "Starting full scan" }
        if (fullScanIsOngoing.get()) {
            log.info { "Full scan already running" }
            return
        }
        fullScanIsOngoing.set(true)
        val libraries = transaction { LibraryEntity.find { LibrariesTable.id inList libraryIDs }.map { it.toModel() } }
        libraries.forEach { library ->
            log.info { "Scanning library ${library.name}" }
            scanLibrary(library)
        }
    }

    override fun scanLibrary(library: Library) {
        log.info { "Scanning library ${library.name}" }
        transaction { library.scanIndex += 1u }

        for (folder in library.folders.map { Paths.get(it) }) {
            scanFolder(folder, library)
        }
        cleanupLibrary(library)
    }

    private fun cleanupLibrary(library: Library) =
        transaction {
            // Remove all tracks that have not been updated
            TrackEntity.find { TracksTable.scanIndex less library.scanIndex }.forEach { it.delete() }
            // Find all books that have no tracks and remove them
            BookEntity.all().filter { it.tracks.empty() }.forEach { it.delete() }
            // Find all authors that have no books and remove them
            AuthorEntity.all().filter { it.books.empty() }.forEach { it.delete() }
            // Find all series that have no books and remove them
            SeriesEntity.all().filter { it.books.empty() }.forEach { it.delete() }
            // Remove images that are not used any more

            // Step 1: Find referenced image IDs
            val referencedImageIds =
                (
                    BooksTable.select(BooksTable.coverID).where { BooksTable.coverID.isNotNull() } +
                        SeriesTable.select(SeriesTable.coverID).where { SeriesTable.coverID.isNotNull() } +
                        AuthorTable.select(AuthorTable.imageID).where { AuthorTable.imageID.isNotNull() }
                ).mapNotNull {
                    it.getOrNull(BooksTable.coverID) ?: it.getOrNull(SeriesTable.coverID)
                        ?: it.getOrNull(AuthorTable.imageID)
                }.distinct()

            // Step 2: Delete unused images
            ImageTable.deleteWhere { ImageTable.id notInList referencedImageIds }
        }

    override fun scanLibrary(library: Library) {
        val dbLib = transaction { LibraryEntity[library.id] }
        scanLibrary(dbLib)
    }

    override fun scanFolder(
        folder: Path,
        library: Library?,
    ) {
        if (isIgnored(folder)) {
            log.info { "Skipping $folder because it is ignored" }
            return
        }

        val selectedLibrary = library ?: transaction { libraryRepository.getMatching(folder) }
        if (selectedLibrary == null) {
            log.error { "No library found for folder $folder. Ignoring folder!" }
            return
        }

        walkFiles(
            folder,
            ignoredSubtree = {
                foldersToIgnore.add(it)
                trackManager.removePath(it)
            },
            shouldUpdateFile = { path -> shouldUpdate(path) },
            addOrUpdate = { path, _ -> runBlocking { addOrUpdatePath(path, selectedLibrary) } },
        )
    }

    override suspend fun addOrUpdatePath(
        path: Path,
        library: Library?,
    ) {
        if (isIgnored(path)) {
            log.info { "Skipping $path because it is in a folder that is ignored" }
            return
        }
        val selectedLibrary = library ?: transaction { libraryRepository.getMatching(path) }
        if (selectedLibrary == null) {
            log.error { "No library found for path $path. Ignoring path!" }
            return
        }

        trackManager.addPath(path, selectedLibrary)
    }

    private fun shouldUpdate(path: Path): Boolean {
        val dbTrack = transaction { TrackEntity.findOne { TracksTable.path like path.absolutePathString() } }
        // If the track has already been imported and the access time has not changed skip
        if (dbTrack != null && !dbTrack.hasBeenUpdated(path.getLastModifiedTime().toMillis())) {
            // Mark as touched, so the tracks don't get removed
            dbTrack.markAsTouched()
            return false
        }
        return true
    }
}
