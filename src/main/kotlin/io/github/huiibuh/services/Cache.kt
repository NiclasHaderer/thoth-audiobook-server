package io.github.huiibuh.services

import io.github.huiibuh.config.Settings
import io.github.huiibuh.db.tables.*
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
            val databases = arrayOf<Table>(TTracks, TBooks, TAuthors, TSeries, TImages)
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
        accessTime = trackRef.lastModified
        trackNr = trackRef.trackNr
        path = trackRef.path
        author = GetOrCreate.author(trackRef.author)
        composer = if (trackRef.narrator != null) GetOrCreate.author(trackRef.narrator!!) else null
        series = if (trackRef.series != null) GetOrCreate.series(trackRef.series!!, author) else null
        book = GetOrCreate.book(trackRef.book,
                                author,
                                composer,
                                series,
                                trackRef.seriesIndex,
                                trackRef.cover)
        seriesIndex = trackRef.seriesIndex
    }
}
