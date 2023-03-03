package io.thoth.server.file.tagger

import java.io.File
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.nameWithoutExtension
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey

interface ReadonlyFileTagger {
    val title: String
    val description: String?
    val date: LocalDate?
    val author: String?
    val book: String?
    val language: String?
    val trackNr: Int?
    val narrator: String?
    val series: String?
    val seriesIndex: Float?
    val cover: ByteArray?
    val duration: Int
    val path: String
    val lastModified: Long
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

    override val description: String?
        get() = audioFile.tag.getFirst(FieldKey.COMMENT).ifEmpty { null }

    override val date: LocalDate?
        get() {
            return try {
                val releaseDate: String? = audioFile.tag.getFirst(FieldKey.ORIGINALRELEASEDATE)
                LocalDate.parse(releaseDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            } catch (e: Exception) {
                val year = audioFile.tag.getFirst(FieldKey.YEAR).toIntOrNull()
                val albumYear = audioFile.tag.getFirst(FieldKey.ALBUM_YEAR).toIntOrNull()

                val finalYear = year ?: albumYear ?: return null
                return LocalDate.of(finalYear, 1, 1)
            }
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
        get() = audioFile.file.normalize().absolutePath

    override val lastModified: Long
        get() = audioFile.file.lastModified()
}
