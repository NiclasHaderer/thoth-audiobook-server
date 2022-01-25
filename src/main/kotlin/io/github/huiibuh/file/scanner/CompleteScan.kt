package io.github.huiibuh.file.scanner

import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.extensions.classLogger
import io.github.huiibuh.extensions.findOne
import io.github.huiibuh.file.persister.FileAnalyzingScheduler
import io.github.huiibuh.settings.Settings
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString
import kotlin.io.path.getLastModifiedTime

val scanIsOngoing = AtomicBoolean()

class CompleteScan(private var basePath: Path? = null) : KoinComponent {
    private val log = classLogger()
    private val settings by inject<Settings>()
    private val fileAnalyzeScheduler by inject<FileAnalyzingScheduler>()

    init {
        if (basePath == null) {
            basePath = Paths.get(settings.audioFileLocation)
        }
    }

    fun start() {
        if (scanIsOngoing.get()) return
        scanIsOngoing.set(true)
        try {
            scanFolderForTracks()
        } finally {
            scanIsOngoing.set(false)
        }
    }


    private fun scanFolderForTracks() {
        log.info("Starting complete scan")
        val scanner = AudioFileScanner(
            removeSubtree = { fileAnalyzeScheduler.queue(FileAnalyzingScheduler.Type.REMOVE_FILE, it) },
            shouldUpdateFile = this::shouldUpdate,
            addOrUpdate = { path, _ ->
                fileAnalyzeScheduler.queue(FileAnalyzingScheduler.Type.ADD_FILE, path)
            }
        )

        Files.walkFileTree(basePath!!, scanner)
    }


    private fun shouldUpdate(path: Path): Boolean {
        val dbTrack = transaction { Track.findOne { TTracks.path eq path.absolutePathString() } }
        // If the track has already been imported and the access time has not changed skip
        if (dbTrack != null && !dbTrack.hasBeenUpdated(path.getLastModifiedTime().toMillis())) {
            // Mark as touched, so the tracks don't get removed
            dbTrack.markAsTouched()
            return false
        }
        return true
    }
}
