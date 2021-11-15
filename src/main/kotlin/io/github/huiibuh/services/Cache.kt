package io.github.huiibuh.services

import io.github.huiibuh.config.Settings
import io.github.huiibuh.db.models.*
import io.github.huiibuh.scanner.TrackReference
import io.github.huiibuh.scanner.traverseAudioFiles
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object Cache {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun reinitialize() {
        clear()
        importTracks()
    }

    private fun clear() {
        logger.info("Clearing cache")
        transaction {
            val databases = arrayOf<Table>(Tracks, Albums, Artists, Collections, Images)
            SchemaUtils.drop(*databases)
            SchemaUtils.create(*databases)
        }
    }

    private fun importTracks() {
        logger.info("Scanning for files")
        traverseAudioFiles(Settings.audioFileLocation) { trackReference, _, _ ->
            transaction {
                createTrack(trackReference)
            }
        }
        logger.info("Scan completed")
    }

    private fun createTrack(trackRef: TrackReference) = Track.new {
        title = trackRef.title
        duration = trackRef.duration
        accessTime = trackRef.lastModfied
        trackNr = trackRef.trackNr
        path = trackRef.path
        artist = GetOrCreate.artist(trackRef.artist)
        composer = if (trackRef.composer != null) GetOrCreate.artist(trackRef.composer!!) else null
        collection = if (trackRef.collection != null) GetOrCreate.collection(trackRef.collection!!, artist) else null
        album = GetOrCreate.album(trackRef.album,
                                  artist,
                                  composer,
                                  collection,
                                  trackRef.collectionIndex,
                                  trackRef.cover)
        collectionIndex = trackRef.collectionIndex
    }
}
