package io.github.huiibuh.file.tagger

import io.github.huiibuh.file.TrackReference
import io.github.huiibuh.models.ProviderIDModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.nio.file.Path


interface FileTagger : ReadonlyFileTagger {
    override var title: String
    override var description: String?
    override var year: Int?
    override val book: String?
    override var language: String?
    override var author: String?
    override var trackNr: Int?
    override var narrator: String?
    override var series: String?
    override var seriesIndex: Float?
    override var cover: ByteArray?
    fun save(): Unit
}


open class FileTaggerImpl(private val audioFile: AudioFile) : ReadonlyFileTaggerImpl(audioFile), FileTagger {
    constructor(path: Path) : this(path.toFile())
    constructor(file: File) : this(AudioFileIO.read(file))
    constructor(path: String) : this(File(path))

    override var title: String
        get() = super.title
        set(value) = audioFile.tag.setField(FieldKey.TITLE, value)

    override var providerId: ProviderIDModel?
        get() = super.providerId
        set(value) {
            val valueStr = Json.encodeToString(value)
            setOrDelete(FieldKey.AMAZON_ID, valueStr)
        }

    override var description: String?
        get() = super.description
        set(value) = setOrDelete(FieldKey.LANGUAGE, value)

    override var year: Int?
        get() = super.year
        set(value) = setOrDelete(FieldKey.YEAR, value)

    override var book: String?
        get() = super.book
        set(value) = setOrDelete(FieldKey.ALBUM, value)

    override var language: String?
        get() = super.language
        set(value) = setOrDelete(FieldKey.LANGUAGE, value)

    override var author: String?
        get() = super.author
        set(value) {
            // Set both for plex
            setOrDelete(FieldKey.ALBUM_ARTIST, value)
            setOrDelete(FieldKey.ARTIST, value)
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

    protected fun setOrDelete(key: FieldKey, value: String?) {
        if (value == null || value.isEmpty()) {
            audioFile.tag.deleteField(key)
        } else {
            audioFile.tag.setField(key, value)
        }
    }

    protected fun setOrDelete(key: FieldKey, value: Int?) {
        setOrDelete(key, value.toString())
    }
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

fun List<FileTaggerImpl>.toTrackModel() = runBlocking {
    val parent = Job()
    val t = this@toTrackModel.map {
        async(parent) {
            FileTaggerImpl(it.path)
        }
    }
    t.map { it.await() }
}
