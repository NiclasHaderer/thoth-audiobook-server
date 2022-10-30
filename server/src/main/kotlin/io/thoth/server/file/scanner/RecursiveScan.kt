package io.thoth.server.file.scanner

import io.thoth.common.extensions.classLogger
import io.thoth.common.extensions.findOne
import io.thoth.database.tables.TTracks
import io.thoth.database.tables.Track
import io.thoth.server.config.ThothConfig
import io.thoth.server.file.persister.FileAnalyzingScheduler
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString
import kotlin.io.path.getLastModifiedTime


class RecursiveScan(private var basePath: List<Path> = listOf()) : KoinComponent {
    private val log = classLogger()
    private val thothConfig by inject<ThothConfig>()
    private val fileAnalyzeScheduler by inject<FileAnalyzingScheduler>()
    private val scanIsOngoing = AtomicBoolean()


    constructor(basePath: Path) : this(listOf(basePath))

    init {
        basePath += thothConfig.audioFileLocation.map { Paths.get(it) }
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
        basePath.forEach {
            Files.walkFileTree(it, scanner)
        }
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
