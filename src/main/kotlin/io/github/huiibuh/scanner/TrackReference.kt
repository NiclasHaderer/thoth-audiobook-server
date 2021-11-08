package io.github.huiibuh.scanner

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File

class TrackReference(private val audioFile: AudioFile) {

    companion object {
        fun fromPath(path: String): TrackReference {
            return TrackReference(
                AudioFileIO.read(File(path))
            )
        }
    }

    var title: String
        get() = audioFile.tag.getFirst(FieldKey.TITLE)
        set(value) {
            audioFile.tag.setField(FieldKey.TITLE, value)
        }
    var trackNr: Int
        get() = audioFile.tag.getFirst(FieldKey.TRACK).toIntOrNull() ?: 0
        set(value) {
            audioFile.tag.setField(FieldKey.TRACK, value.toString())
        }
    var album: String
        get() = audioFile.tag.getFirst(FieldKey.ALBUM)
        set(value) {
            audioFile.tag.setField(FieldKey.ALBUM, value)
        }
    var artist: String
        get() = audioFile.tag.getFirst(FieldKey.ARTIST)
        set(value) {
            audioFile.tag.setField(FieldKey.ALBUM_ARTIST, value)
            audioFile.tag.setField(FieldKey.ARTIST, value)
        }
    var composer: String?
        get() = audioFile.tag.getFirst(FieldKey.COMPOSER)
        set(value) {
            audioFile.tag.setField(FieldKey.COMPOSER, value)
        }
    var collection: String?
        get() = audioFile.tag.getFirst(FieldKey.GROUPING)
        set(value) {
            audioFile.tag.setField(FieldKey.GROUPING, value)
        }
    var collectionIndex: Int?
        get() = audioFile.tag.getFirst(FieldKey.CATALOG_NO).toIntOrNull()
        set(value) {
            audioFile.tag.setField(FieldKey.CATALOG_NO, value.toString())
        }
    var cover: ByteArray?
        get() = audioFile.tag.firstArtwork.binaryData
        set(value) {
            val artwork = ArtworkFactory.getNew()
            artwork.binaryData = value
            audioFile.tag.setField(artwork)
        }

    val coverInformation
        get() = CoverInformation(audioFile.tag.firstArtwork.binaryData, audioFile.tag.firstArtwork.mimeType)

    val duration: Int
        get() = audioFile.audioHeader.trackLength

    fun save() {
        AudioFileIO.write(this.audioFile)
    }
}
