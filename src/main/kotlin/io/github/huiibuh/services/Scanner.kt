package io.github.huiibuh.services

import io.github.huiibuh.db.removeAllUnusedFromDb
import io.github.huiibuh.db.tables.KeyValueSettings
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.extensions.classLogger
import io.github.huiibuh.extensions.findOne
import io.github.huiibuh.file.analyzer.AudioFileAnalysisResult
import io.github.huiibuh.file.analyzer.AudioFileAnalyzerWrapper
import io.github.huiibuh.file.scanner.AudioFileScanner
import io.github.huiibuh.settings.Settings
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.getLastModifiedTime


object Scanner : KoinComponent {
    private val isScanning = AtomicBoolean(false)
    private val logger = classLogger()
    private val settings by inject<Settings>()
    private val fileAnalyzer by inject<AudioFileAnalyzerWrapper>()

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

        val file = Paths.get(settings.audioFileLocation)

        // Import tracks
        scanFolderForTracks(file)

        // Remove tracks which where not found and the scanIndex could therefore not be updated
        Track.removeUntouched()

        // Update the scan index after removing all tracks which are no longer valid
        KeyValueSettings.get().incrementScanIndex()

        // Remove all empty collections
        logger.info("Removing empty series, authors and albums")
        removeAllUnusedFromDb()

        // Print some statistics
        val finishedAt = System.currentTimeMillis()
        val elapsedTimeInSeconds = (finishedAt - initializedStartAt) / 1_000.0
        logger.info("Database maintenance took $elapsedTimeInSeconds seconds.")
    }

    fun fileDeleted(path: Path) {
        transaction { Track.findOne { TTracks.path eq path.toString() }?.delete() }
        removeAllUnusedFromDb()
    }

    fun fileCreated(path: Path) {
        if (!shouldUpdate(path)) return
        val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
        val trackInfo = runBlocking { fileAnalyzer.analyze(path, attrs) } ?: return
        addOrUpdate(path, attrs, trackInfo)
    }

    fun scanFolderForTracks(basePath: Path) {
        val scanner = AudioFileScanner(
            fileAnalyzer,
            removeSubtree = { path ->
                transaction {
                    Track.find { TTracks.path like "${path.absolute()}%" }
                            .forEach { it.delete() }
                }
            },
            shouldUpdateFile = Scanner::shouldUpdate,
            addOrUpdate = Scanner::addOrUpdate
        )

        Files.walkFileTree(basePath, scanner)
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


    private fun addOrUpdate(path: Path, attrs: BasicFileAttributes, analysisResult: AudioFileAnalysisResult) {
        val dbTrack = transaction { Track.findOne { TTracks.path eq path.absolutePathString() } }
        runBlocking {
            if (dbTrack != null) {
                updateTrack(analysisResult, dbTrack)
            } else {
                createTrack(analysisResult)
            }
        }
    }

    private fun createTrack(trackRef: AudioFileAnalysisResult) = transaction {
        val kvSettings = KeyValueSettings.get()
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
            this.scanIndex = kvSettings.scanIndex + 1
        }
    }

    private fun updateTrack(trackRef: AudioFileAnalysisResult, track: Track) = transaction {
        val kvSettings = KeyValueSettings.get()

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
            this.scanIndex = kvSettings.scanIndex + 1
        }
    }
}
