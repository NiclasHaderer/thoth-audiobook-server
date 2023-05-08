package io.thoth.server.file.scanner

import io.thoth.models.LibraryModel
import io.thoth.server.common.extensions.findOne
import io.thoth.server.database.access.hasBeenUpdated
import io.thoth.server.database.access.markAsTouched
import io.thoth.server.database.access.toModel
import io.thoth.server.database.tables.Library
import io.thoth.server.database.tables.TTracks
import io.thoth.server.database.tables.Track
import io.thoth.server.file.TrackManager
import io.thoth.server.services.LibraryRepository
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString
import kotlin.io.path.getLastModifiedTime
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging.logger
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface LibraryScanner {
    fun fullScan()
    fun scanLibrary(library: LibraryModel)
    fun scanLibrary(library: Library)
    fun scanFolder(folder: Path, library: Library?)
    fun unIgnoreFolder(folder: Path)
    fun ignoreFolder(folder: Path)
    fun removePath(path: Path)
    fun isIgnored(folder: Path): Boolean
    suspend fun addOrUpdatePath(path: Path, library: Library?)
}

class LibraryScannerImpl : LibraryScanner, KoinComponent {
    private val trackManager: TrackManager by inject()
    private val libraryRepository: LibraryRepository by inject()
    // TODO if there should be a lock on scanning a library/folder/everything

    companion object {
        private val fullScanIsOngoing = AtomicBoolean()
        private val foldersToIgnore = mutableSetOf<Path>()
        private val log = logger {}
    }

    override fun isIgnored(folder: Path): Boolean {
        return foldersToIgnore.any { it.startsWith(folder) }
    }

    override fun unIgnoreFolder(folder: Path) {
        foldersToIgnore.removeIf { it.absolutePathString() == folder.absolutePathString() }
    }

    override fun ignoreFolder(folder: Path) {
        if (!foldersToIgnore.any { it.absolutePathString() == folder.absolutePathString() }) {
            return
        }
        foldersToIgnore.add(folder)
        removePath(folder)
        // TODO schedule a cleanup to remove empty albums, artists, etc.
    }

    override fun removePath(path: Path) {
        trackManager.removePath(path)
    }

    override fun fullScan() {
        log.info { "Starting full scan" }
        if (fullScanIsOngoing.get()) {
            log.info { "Full scan already running" }
            return
        }
        fullScanIsOngoing.set(true)
        log.info { "Starting complete scan" }
        val libraries = transaction { Library.all().map { it.toModel() } }
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
    }

    override fun scanLibrary(library: LibraryModel) {
        val dbLib = transaction { Library[library.id] }
        scanLibrary(dbLib)
    }

    override fun scanFolder(folder: Path, library: Library?) {
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

    override suspend fun addOrUpdatePath(path: Path, library: Library?) {
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
        val dbTrack = transaction { Track.findOne { TTracks.path like path.absolutePathString() } }
        // If the track has already been imported and the access time has not changed skip
        if (dbTrack != null && !dbTrack.hasBeenUpdated(path.getLastModifiedTime().toMillis())) {
            // Mark as touched, so the tracks don't get removed
            dbTrack.markAsTouched()
            return false
        }
        return true
    }
}
