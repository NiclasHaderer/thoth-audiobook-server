package io.thoth.server.file.scanner

import io.thoth.common.extensions.findOne
import io.thoth.common.extensions.get
import io.thoth.database.access.hasBeenUpdated
import io.thoth.database.access.markAsTouched
import io.thoth.database.access.toModel
import io.thoth.database.tables.Library
import io.thoth.database.tables.TTracks
import io.thoth.database.tables.Track
import io.thoth.server.file.persister.FileAnalyzingScheduler
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString
import kotlin.io.path.getLastModifiedTime
import mu.KotlinLogging.logger
import org.jetbrains.exposed.sql.transactions.transaction

private val scanIsOngoing = AtomicBoolean()

fun fullScan() {
    val log = logger {}
    log.info { "Starting full scan" }
    if (scanIsOngoing.get()) {
        log.info { "Full scan already running" }
        return
    }
    scanIsOngoing.set(true)
    log.info { "Starting complete scan" }
    scanFoldersForTracks()
}

fun scanFoldersForTracks() {
    val fileAnalyzeScheduler = get<FileAnalyzingScheduler>()
    val libraries = transaction { Library.all().map { it.toModel() } }
    libraries
        .flatMap { it.folders.map { folder -> Pair(it, Paths.get(folder)) } }
        .forEach { (library, folder) ->
            val scanner =
                AudioFileScanner(
                    removeSubtree = {
                        fileAnalyzeScheduler.queue(FileAnalyzingScheduler.Type.REMOVE_FILE, it)
                    },
                    shouldUpdateFile = ::shouldUpdate,
                    addOrUpdate = { path, _ ->
                        fileAnalyzeScheduler.queue(FileAnalyzingScheduler.Type.ADD_FILE, path)
                    },
                )
            Files.walkFileTree(folder, scanner)
            // TODO increase the scan index
            // TODO remove all tracks that have not been touched and are in said library
        }
}

fun shouldUpdate(path: Path): Boolean {
    val dbTrack = transaction { Track.findOne { TTracks.path like path.absolutePathString() } }
    // If the track has already been imported and the access time has not changed skip
    if (dbTrack != null && !dbTrack.hasBeenUpdated(path.getLastModifiedTime().toMillis())) {
        // Mark as touched, so the tracks don't get removed
        dbTrack.markAsTouched()
        return false
    }
    return true
}
