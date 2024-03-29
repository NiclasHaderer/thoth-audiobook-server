package io.thoth.server.file.tagger

import io.thoth.server.database.tables.Track
import java.io.File
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory

interface FileTagger : ReadonlyFileTagger {
    override var title: String
    override var description: String?
    override var date: LocalDate?
    override val book: String?
    override var language: String?
    override var authors: List<String>?
    override var trackNr: Int?
    override var narrator: String?
    override var series: String?
    override var seriesIndex: Float?
    override var cover: ByteArray?

    fun save()
}

open class FileTaggerImpl(private val audioFile: AudioFile) : ReadonlyFileTaggerImpl(audioFile), FileTagger {
    constructor(path: Path) : this(path.toFile())

    constructor(file: File) : this(AudioFileIO.read(file))

    constructor(path: String) : this(File(path))

    override var title: String
        get() = super.title
        set(value) = audioFile.tag.setField(FieldKey.TITLE, value)

    override var description: String?
        get() = super.description
        set(value) = setOrDelete(FieldKey.LANGUAGE, value)

    override var date: LocalDate?
        get() = super.date
        set(value) {
            setOrDelete(FieldKey.YEAR, value?.format(DateTimeFormatter.ofPattern("yyyy")))
            setOrDelete(FieldKey.ORIGINALRELEASEDATE, value?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        }

    override var book: String?
        get() = super.book
        set(value) = setOrDelete(FieldKey.ALBUM, value)

    override var language: String?
        get() = super.language
        set(value) = setOrDelete(FieldKey.LANGUAGE, value)

    override var authors: List<String>?
        get() = super.authors
        set(value) {
            val joinedValue = value?.joinToString(Char.MIN_VALUE.toString()) { it }
            // Set both for plex
            setOrDelete(FieldKey.ALBUM_ARTIST, joinedValue)
            setOrDelete(FieldKey.ARTIST, joinedValue)
        }

    override var trackNr: Int?
        get() = super.trackNr
        set(value) = setOrDelete(FieldKey.TRACK, value)

    override var narrator: String?
        get() = super.narrator
        set(value) = setOrDelete(FieldKey.COMPOSER, value)

    override var series: String?
        get() = audioFile.tag.getFirst(FieldKey.GROUPING).ifEmpty { null }
        set(value) = setOrDelete(FieldKey.GROUPING, value)

    override var seriesIndex: Float?
        get() = audioFile.tag.getFirst(FieldKey.CATALOG_NO).toFloatOrNull()
        set(value) = setOrDelete(FieldKey.CATALOG_NO, value?.toString())

    override var cover: ByteArray?
        get() = audioFile.tag.firstArtwork?.binaryData
        set(value) {
            if (value == null) {
                return audioFile.tag.deleteArtworkField()
            }
            val artwork = ArtworkFactory.getNew()
            artwork.binaryData = value
            audioFile.tag.setField(artwork)
        }

    override fun save() {
        AudioFileIO.write(this.audioFile)
    }

    private fun setOrDelete(key: FieldKey, value: String?) {
        if (value == null || value.isEmpty()) {
            audioFile.tag.deleteField(key)
        } else {
            audioFile.tag.setField(key, value)
        }
    }

    private fun setOrDelete(key: FieldKey, value: Int?) {
        setOrDelete(key, value.toString())
    }
}

fun List<FileTagger>.saveToFile() = runBlocking {
    val parent = Job()
    this@saveToFile.forEach { launch(parent) { it.save() } }
    parent.children.forEach { it.join() }
}

fun List<Track>.toTrackModel() = runBlocking {
    val parent = Job()
    val t = this@toTrackModel.map { async(parent) { FileTaggerImpl(it.path) } }
    t.map { it.await() }
}
