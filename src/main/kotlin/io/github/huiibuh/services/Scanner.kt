package io.github.huiibuh.services

import io.github.huiibuh.config.Settings
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.models.SharedSettings
import io.github.huiibuh.scanner.TrackReference
import io.github.huiibuh.scanner.traverseAudioFiles
import io.github.huiibuh.services.database.SharedSettingsService
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

val isScanning = AtomicBoolean(false)

object Scanner {
    private val logger = LoggerFactory.getLogger(this::class.java)

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

        val settings = SharedSettingsService.get()
        // Import tracks
        importTracks(settings)
        // Remove tracks which where not found and the scanIndex could therefore not be updated
        transaction {
            Track.find { TTracks.scanIndex eq settings.scanIndex }
                    .forEach { it.delete() }
        }
        // Update the scan index
        settings.scanIndex += 1
        SharedSettingsService.save(settings)
        logger.info("Removing empty series, authors and albums")
        // Remove all empty collections
        RemoveEmpty.all()

        val finishedAt = System.currentTimeMillis()
        val elapsedTimeInSeconds = (finishedAt - initializedStartAt) / 1_000.0
        logger.info("Database maintenance took $elapsedTimeInSeconds seconds.")
    }

    private fun importTracks(settings: SharedSettings) {
        traverseAudioFiles(
            Settings.audioFileLocation,
            add = { trackReference, _, _, track ->
                if (track != null) {
                    updateTrack(trackReference, track, settings.scanIndex + 1)
                } else {
                    createTrack(trackReference, settings.scanIndex + 1)
                }
            }, removeSubtree = { path ->
                transaction { Track.find { TTracks.path like "$path%" }.forEach { it.delete() } }
            })

    }

    private fun createTrack(trackRef: TrackReference, scanIndex: Int) = transaction {

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
            this.scanIndex = scanIndex
        }
    }

    private fun updateTrack(trackRef: TrackReference, track: Track, scanIndex: Int) = transaction {
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
            this.scanIndex = scanIndex
        }
    }
}
