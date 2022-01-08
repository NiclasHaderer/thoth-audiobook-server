package io.github.huiibuh.services

import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.extensions.findOne
import io.github.huiibuh.file.analyzer.AudioFileAnalysisValue
import io.github.huiibuh.file.analyzer.AudioFileAnalyzer
import io.github.huiibuh.file.scanner.AudioFileScanner
import io.github.huiibuh.models.KeyValueSettings
import io.github.huiibuh.services.database.KeyValueSettingsService
import io.github.huiibuh.settings.Settings
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString
import kotlin.io.path.getLastModifiedTime

val isScanning = AtomicBoolean(false)

object Scanner : KoinComponent {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val settings by inject<Settings>()
    private val fileAnalyzer by inject<AudioFileAnalyzer>()

    fun rescan() {
        if (isScanning.get()) return
        isScanning.set(true)
        try {
            rescanFiles()
        } finally {
            isScanning.set(false)
        }
    }

    private fun rescanFiles() {
        logger.info("Starting import of tracks")
        val initializedStartAt = System.currentTimeMillis()

        val kvSettings = KeyValueSettingsService.get()

        // Import tracks
        importTracks(kvSettings)

        // Remove tracks which where not found and the scanIndex could therefore not be updated
        transaction {
            Track.find { TTracks.scanIndex eq kvSettings.scanIndex }
                    .forEach { it.delete() }
        }

        // Update the scan index
        kvSettings.scanIndex += 1
        KeyValueSettingsService.save(kvSettings)

        // Remove all empty collections
        logger.info("Removing empty series, authors and albums")
        RemoveEmpty.all()

        // Print some statistics
        val finishedAt = System.currentTimeMillis()
        val elapsedTimeInSeconds = (finishedAt - initializedStartAt) / 1_000.0
        logger.info("Database maintenance took $elapsedTimeInSeconds seconds.")
    }

    private fun importTracks(sharedSettings: KeyValueSettings) {
        val scanner = AudioFileScanner(
            fileAnalyzer,
            removeSubtree = { path ->
                transaction { Track.find { TTracks.path like "$path%" }.forEach { it.delete() } }
            },
            shouldUpdateFile = {
                val dbTrack = transaction { Track.findOne { TTracks.path eq it.absolutePathString() } }
                // If the track has already been imported and the access time has not changed skip
                if (dbTrack != null && dbTrack.accessTime >= it.getLastModifiedTime().toMillis()) {
                    // Mark as touched, so the tracks don't get removed
                    transaction { dbTrack.scanIndex = sharedSettings.scanIndex + 1 }
                    return@AudioFileScanner false
                }
                return@AudioFileScanner true
            },
            addOrUpdate = { path: Path, _: BasicFileAttributes, analysisResult: AudioFileAnalysisValue ->
                val dbTrack = transaction { Track.findOne { TTracks.path eq path.absolutePathString() } }
                if (dbTrack != null) {
                    this.updateTrack(analysisResult, dbTrack)
                } else {
                    this.createTrack(analysisResult)
                }
            }
        )

        val file = Paths.get(settings.audioFileLocation)
        Files.walkFileTree(file, scanner)
    }

    private fun createTrack(trackRef: AudioFileAnalysisValue) = transaction {
        val kvSettings = KeyValueSettingsService.get()

        Track.new {
            title = trackRef.title
            duration = trackRef.duration
            accessTime = trackRef.lastModified
            trackNr = trackRef.trackNr
            path = trackRef.path
            book = GetOrCreate.book(title = trackRef.book,
                                    year = trackRef.year,
                                    description = trackRef.description,
                                    language = trackRef.language,
                                    providerID = trackRef.providerId,
                                    series = trackRef.series,
                                    seriesIndex = trackRef.seriesIndex,
                                    cover = trackRef.cover,
                                    author = trackRef.author,
                                    narrator = trackRef.narrator
            )
            this.scanIndex = kvSettings.scanIndex
        }
    }

    private fun updateTrack(trackRef: AudioFileAnalysisValue, track: Track) = transaction {
        val kvSettings = KeyValueSettingsService.get()

        track.apply {
            title = trackRef.title
            duration = trackRef.duration
            accessTime = trackRef.lastModified
            trackNr = trackRef.trackNr
            path = trackRef.path
            book = GetOrCreate.book(title = trackRef.book,
                                    year = trackRef.year,
                                    description = trackRef.description,
                                    language = trackRef.language,
                                    providerID = trackRef.providerId,
                                    series = trackRef.series,
                                    seriesIndex = trackRef.seriesIndex,
                                    cover = trackRef.cover,
                                    author = trackRef.author,
                                    narrator = trackRef.narrator
            )
            this.scanIndex = kvSettings.scanIndex
        }
    }
}
