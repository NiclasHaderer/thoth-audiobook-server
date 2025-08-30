package io.thoth.server.file.scanner

import io.thoth.server.common.extensions.findOne
import io.thoth.server.common.extensions.withGuard
import io.thoth.server.database.access.hasBeenUpdated
import io.thoth.server.database.access.markAsTouched
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookEntity
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageTable
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TrackEntity
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.file.TrackManager
import kotlinx.coroutines.sync.Mutex
import mu.KotlinLogging.logger
import org.jetbrains.exposed.v1.core.statements.UpsertSqlExpressionBuilder.less
import org.jetbrains.exposed.v1.core.statements.UpsertSqlExpressionBuilder.notInSubQuery
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.union
import org.koin.core.component.KoinComponent
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import kotlin.io.path.absolutePathString
import kotlin.io.path.getLastModifiedTime

interface LibraryScanner {
    fun scanLibrary(library: LibraryEntity)
}

class LibraryScannerImpl :
    LibraryScanner,
    KoinComponent {
    companion object {
        private val mutex = Mutex()
        private val currentLibraryScans = mutableMapOf<UUID, Boolean>()
        private val ignoredFolders = mutableListOf<Path>()
        private val log = logger {}

        fun isIgnored(folder: Path): Boolean =
            mutex.withGuard {
                ignoredFolders.any { it.startsWith(folder) }
            }

        fun removeIgnoredFolder(folder: Path): Unit =
            mutex.withGuard {
                ignoredFolders.removeIf { it.absolutePathString() == folder.absolutePathString() }
            }

        // TODO we never un-ignore the folder
        fun ignoreFolder(folder: Path): Unit =
            mutex.withGuard {
                if (!ignoredFolders.any { it.absolutePathString() == folder.absolutePathString() }) {
                    return
                }
                ignoredFolders.add(folder)
            }
    }

    override fun scanLibrary(library: LibraryEntity) {
        try {
            log.info { "Scanning library ${library.name}" }
            mutex.withGuard {
                if (currentLibraryScans[library.id.value] == true) {
                    log.info { "Skipping scan for library ${library.name}. Scan is already ongoing." }
                    return
                }
                currentLibraryScans[library.id.value] = true
            }
            transaction { library.scanIndex += 1u }

            for (folder in library.folders.map { Paths.get(it) }) {
                scanFolder(folder, library)
            }
            cleanupLibrary(library)
        } finally {
            mutex.withGuard {
                require(currentLibraryScans[library.id.value] == true, { "Library scan should have been ongoing" })
                currentLibraryScans[library.id.value] = false
            }
        }
    }

    private fun cleanupLibrary(library: LibraryEntity): Unit =
        transaction {
            // TODO check if there is a better way...
            // Remove all tracks that have not been updated
            TracksTable.deleteWhere { TracksTable.scanIndex less library.scanIndex }
            // Find all books that have no tracks and remove them
            BookEntity.all().filter { it.tracks.empty() }.forEach { it.delete() }
            // Find all authors that have no books and remove them
            AuthorEntity.all().filter { it.books.empty() }.forEach { it.delete() }
            // Find all series that have no books and remove them
            SeriesEntity.all().filter { it.books.empty() }.forEach { it.delete() }

            // Delete unused images
            ImageTable.deleteWhere {
                ImageTable.id notInSubQuery (
                    BooksTable
                        .select(BooksTable.coverID)
                        .where { BooksTable.coverID.isNotNull() }
                        .union(
                            SeriesTable.select(SeriesTable.coverID).where { SeriesTable.coverID.isNotNull() },
                        ).union(
                            AuthorTable.select(AuthorTable.imageID).where { AuthorTable.imageID.isNotNull() },
                        )
                )
            }
        }

    fun scanFolder(
        folder: Path,
        library: LibraryEntity,
    ) {
        if (isIgnored(folder)) {
            log.info { "Skipping '$folder' because it is ignored" }
            return
        }

        walkFiles(
            folder,
            ignoreFolder = {
                ignoreFolder(it)
                TrackManager.removeFolder(it, library)
            },
            addOrUpdate = { path, _ ->
                if (shouldUpdate(path) && !isIgnored(path)) {
                    TrackManager.addPath(path, library)
                }
            },
        )
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
