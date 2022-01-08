package io.github.huiibuh.file.tagger

import io.github.huiibuh.models.ProviderIDModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension


interface ReadonlyFileTagger {
    val title: String
    val description: String?
    val year: Int?
    val author: String?
    val book: String?
    val language: String?
    val trackNr: Int?
    val narrator: String?
    val series: String?
    val seriesIndex: Float?
    val cover: ByteArray?
    val duration: Int?
    val path: String?
    val lastModified: Long?
}


open class ReadonlyFileTaggerImpl(private val audioFile: AudioFile) : ReadonlyFileTagger {
    constructor(path: Path) : this(path.toFile())
    constructor(file: File) : this(AudioFileIO.read(file))
    constructor(path: String) : this(File(path))

    override val title: String
        get() {
            val title = audioFile.tag.getFirst(FieldKey.TITLE).ifEmpty { null }
            return title ?: Path.of(path).nameWithoutExtension
        }

    open val providerId: ProviderIDModel?
        get() {
            return try {
                Json.decodeFromString(audioFile.tag.getFirst(FieldKey.AMAZON_ID))
            } catch (e: Exception) {
                null
            }
        }

    override val description: String?
        get() = audioFile.tag.getFirst(FieldKey.COMMENT).ifEmpty { null }

    override val year: Int?
        get() {
            val year = audioFile.tag.getFirst(FieldKey.YEAR).toIntOrNull()
            val albumYear = audioFile.tag.getFirst(FieldKey.ALBUM_YEAR).toIntOrNull()
            return year ?: albumYear
        }

    override val author: String?
        get() = audioFile.tag.getFirst(FieldKey.ARTIST).ifEmpty { null }

    override val book: String?
        get() = audioFile.tag.getFirst(FieldKey.ALBUM).ifEmpty { null }

    override val language: String?
        get() = audioFile.tag.getFirst(FieldKey.LANGUAGE).ifEmpty { null }

    override val trackNr: Int?
        get() = audioFile.tag.getFirst(FieldKey.TRACK).toIntOrNull()

    override val narrator: String?
        get() = audioFile.tag.getFirst(FieldKey.COMPOSER).ifEmpty { null }

    override val series: String?
        get() = audioFile.tag.getFirst(FieldKey.GROUPING).ifEmpty { null }

    override val seriesIndex: Float?
        get() = audioFile.tag.getFirst(FieldKey.CATALOG_NO).toFloatOrNull()

    override val cover: ByteArray?
        get() = audioFile.tag.firstArtwork?.binaryData

    override val duration: Int
        get() = audioFile.audioHeader.trackLength

    override val path: String
        get() = audioFile.file.absolutePath

    override val lastModified: Long
        get() = audioFile.file.lastModified()

}

