package io.github.huiibuh.scanner

import api.exceptions.APINotFound
import io.github.huiibuh.db.tables.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

class TrackReference(private val audioFile: AudioFile) {

    companion object {
        fun fromPath(path: String): TrackReference {
            val file = File(path)
            if (!file.exists()) throw APINotFound("Requested file was not found. Database out of sync. Please start syncing process.")
            return TrackReference(
                AudioFileIO.read(file)
            )
        }

        fun fromFile(file: File): TrackReference {
            if (!file.exists()) throw APINotFound("Requested file was not found. Database out of sync. Please start syncing process.")
            return TrackReference(
                AudioFileIO.read(file)
            )
        }
    }

    var asin: String?
        get() = stringOrNull(audioFile.tag.getFirst(FieldKey.AMAZON_ID))
        set(value) {
            audioFile.tag.setField(FieldKey.AMAZON_ID, value)
        }

    var description: String?
        get() = stringOrNull(audioFile.tag.getFirst(FieldKey.COMMENT))
        set(value) {
            audioFile.tag.setField(FieldKey.COMMENT, value)
        }

    var language: String?
        get() = stringOrNull(audioFile.tag.getFirst(FieldKey.LANGUAGE))
        set(value) {
            audioFile.tag.setField(FieldKey.LANGUAGE, value)
        }

    var year: Int?
        get() {
            val year = audioFile.tag.getFirst(FieldKey.YEAR).toIntOrNull()
            val albumYear = audioFile.tag.getFirst(FieldKey.YEAR).toIntOrNull()
            return year ?: albumYear
        }
        set(value) {
            audioFile.tag.setField(FieldKey.YEAR, value.toString())
        }

    var title: String
        get() {
            val title = stringOrNull(audioFile.tag.getFirst(FieldKey.TITLE))
            return title ?: Path.of(path).nameWithoutExtension
        }
        set(value) {
            audioFile.tag.setField(FieldKey.TITLE, value)
        }
    var book: String
        get() = audioFile.tag.getFirst(FieldKey.ALBUM)
        set(value) {
            audioFile.tag.setField(FieldKey.ALBUM, value)
        }
    var author: String
        get() = audioFile.tag.getFirst(FieldKey.ARTIST)
        set(value) {
            audioFile.tag.setField(FieldKey.ALBUM_ARTIST, value)
            audioFile.tag.setField(FieldKey.ARTIST, value)
        }
    var trackNr: Int?
        get() = audioFile.tag.getFirst(FieldKey.TRACK).toIntOrNull()
        set(value) {
            audioFile.tag.setField(FieldKey.TRACK, value.toString())
        }
    var narrator: String?
        get() = stringOrNull(audioFile.tag.getFirst(FieldKey.COMPOSER))
        set(value) {
            audioFile.tag.setField(FieldKey.COMPOSER, value)
        }
    var series: String?
        get() = stringOrNull(audioFile.tag.getFirst(FieldKey.GROUPING))
        set(value) {
            audioFile.tag.setField(FieldKey.GROUPING, value)
        }
    var seriesIndex: Int?
        get() = audioFile.tag.getFirst(FieldKey.CATALOG_NO).toIntOrNull()
        set(value) {
            audioFile.tag.setField(FieldKey.CATALOG_NO, value.toString())
        }
    var cover: ByteArray?
        get() = audioFile.tag.firstArtwork?.binaryData
        set(value) {
            val artwork = ArtworkFactory.getNew()
            artwork.binaryData = value
            audioFile.tag.setField(artwork)
        }

    val coverInformation
        get() = CoverInformation(audioFile.tag.firstArtwork.binaryData, audioFile.tag.firstArtwork.mimeType)

    val duration: Int
        get() = audioFile.audioHeader.trackLength

    val path: String
        get() = audioFile.file.absolutePath

    val lastModified: Long
        get() = audioFile.file.lastModified()

    fun save() {
        AudioFileIO.write(this.audioFile)
    }

    fun hasRequiredAttributes(): Boolean {
        val author: String? = audioFile.tag.getFirst(FieldKey.ARTIST)
        val book: String? = audioFile.tag.getFirst(FieldKey.ALBUM)
        return author != null && author.trim().isNotEmpty() && book != null && book.trim().isNotEmpty()
    }

    private fun stringOrNull(str: String): String? {
        return if (str.isEmpty()) null else str
    }

}


data class CoverInformation(
    val cover: ByteArray,
    val mimetype: String,
) {
    val extension: String
        get() = mimetype.split("/").last()
}


fun List<TrackReference>.saveToFile() = runBlocking {
    val parent = Job()
    this@saveToFile.forEach {
        launch(parent) {
            it.save()
        }
    }
    parent.children.forEach { it.join() }
}

fun List<Track>.toTrackModel() = runBlocking {
    val parent = Job()
    val t = this@toTrackModel.map {
        async(parent) {
            TrackReference.fromPath(it.path)
        }
    }
    t.map { it.await() }
}
