package io.github.huiibuh.services

import io.github.huiibuh.config.Settings
import io.github.huiibuh.db.findOne
import io.github.huiibuh.db.models.Track
import io.github.huiibuh.db.models.Tracks
import io.github.huiibuh.scanner.TrackReference
import io.github.huiibuh.scanner.fileExists
import io.github.huiibuh.scanner.traverseAudioFiles
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.getLastModifiedTime

object DB {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun runValidation() {
        log.info("Validating current database")
        transaction {
            removeAndupdateDBTracks()
        }
        log.info("Removing empty albums")
        RemoveEmpty.albums()
        log.info("Removing empty artists")
        RemoveEmpty.artists()
        log.info("Removing empty collections")
        RemoveEmpty.collections()
    }

    private fun removeAndupdateDBTracks() {
        Track.all().forEach {
            if (!fileExists(it.path)) {
                it.delete()
                return
            }
            val file = Paths.get(it.path)
            val time = file.getLastModifiedTime().to(TimeUnit.MILLISECONDS)
            if (time != it.accessTime) {
                updateTrack(it)
            }
        }
    }

    fun updateTrack(track: Track, trackRef: TrackReference? = null, fileRef: Path? = null) {
        val file = fileRef ?: Paths.get(track.path)
        val trackReference = trackRef ?: TrackReference.fromPath(track.path)
        track.apply {
            title = trackReference.title
            trackNr = trackReference.trackNr
            duration = trackReference.duration
            accessTime = file.getLastModifiedTime().to(TimeUnit.MILLISECONDS)
            artist = GetOrCreate.artist(trackReference.artist)
            collection =
                if (trackReference.collection != null) GetOrCreate.collection(trackReference.collection!!,
                                                                              artist) else null
            composer = if (trackReference.composer != null) GetOrCreate.artist(trackReference.composer!!) else null
            album = GetOrCreate.album(trackReference.album,
                                      artist,
                                      composer,
                                      collection,
                                      trackReference.collectionIndex,
                                      trackReference.cover)
            collectionIndex = trackReference.collectionIndex
        }
        track.flush()
    }

    fun importMissingTracks() {
        log.info("Scanning for new files")
        traverseAudioFiles(Settings.audioFileLocation) { trackReference, _, path ->
            transaction {
                val track = Track.findOne { Tracks.path eq trackReference.path }
                if (track == null) {
                    val newTrack = createMinimalNewTrack(trackReference, path)
                    updateTrack(newTrack, trackReference, path)
                }
            }
        }

    }

    private fun createMinimalNewTrack(trackRef: TrackReference, path: Path): Track {
        return Track.new {
            this.path = path.toAbsolutePath().toString()
            artist = GetOrCreate.artist(trackRef.artist)

            album = GetOrCreate.album(trackRef.album, artist, null, null, null, null)
        }
    }
}
