package io.thoth.server.file.scanner

import io.thoth.common.extensions.findOne
import io.thoth.common.scheduling.Scheduler
import io.thoth.database.access.hasBeenUpdated
import io.thoth.database.access.markAsTouched
import io.thoth.database.access.toModel
import io.thoth.database.tables.Library
import io.thoth.database.tables.TTracks
import io.thoth.database.tables.Track
import io.thoth.models.LibraryModel
import io.thoth.server.file.persister.AudioAnalyzer
import io.thoth.server.scheduler.ThothSchedules
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString
import kotlin.io.path.getLastModifiedTime
import mu.KotlinLogging.logger
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface LibraryScanner {
    fun fullScan()
    fun scanLibrary(library: LibraryModel)
    fun scanFolder(folder: Path)
}

class LibraryScannerImpl : LibraryScanner, KoinComponent {
    private val audioAnalyzer: AudioAnalyzer by inject()
    private val scheduler: Scheduler by inject()
    private val thothSchedules: ThothSchedules by inject()
    // TODO if there should be a lock on scanning a library/folder/everything

    companion object {
        private val fullScanIsOngoing = AtomicBoolean()
        private val foldersToIgnore = mutableSetOf<String>()
        private val log = logger {}
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

    override fun scanLibrary(library: LibraryModel) {
        for (folder in library.folders.map { Paths.get(it) }) {
            scanFolder(folder)
        }
    }

    override fun scanFolder(folder: Path) {
        val scanner =
            FileTreeScanner(
                ignoredSubtree = {
                    foldersToIgnore.add(it.absolutePathString())
                    audioAnalyzer.queue(AudioAnalyzer.Type.REMOVE_FILE, it)
                },
                shouldUpdateFile = ::shouldUpdate,
                addOrUpdate = { path, _ -> audioAnalyzer.queue(AudioAnalyzer.Type.ADD_FILE, path) },
            )
        Files.walkFileTree(folder, scanner)
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
