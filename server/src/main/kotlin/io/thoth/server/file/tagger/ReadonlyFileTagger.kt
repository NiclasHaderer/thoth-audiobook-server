package io.thoth.server.file.tagger

import io.thoth.taglib.TagLibFile
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.absolute
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.nameWithoutExtension

/**
 * Reads the audiobook fields out of TagLib's normalized property map. All native access happens in
 * the constructor so nothing has to be closed by callers.
 */
class ReadonlyFileTagger(
    filePath: Path,
) {
    constructor(path: String) : this(Path.of(path))

    private val properties: Map<String, List<String>>

    val cover: ByteArray?
    val duration: Int
    val path: String = filePath.absolute().normalize().toString()
    val lastModified: Long = filePath.getLastModifiedTime().toMillis()

    init {
        TagLibFile(filePath).use { file ->
            properties = file.properties()
            cover = file.pictures().firstOrNull()?.data
            duration = file.lengthInSeconds
        }
    }

    val title: String
        get() = first("TITLE") ?: Path.of(path).nameWithoutExtension

    val description: String?
        get() = first("COMMENT")

    val date: LocalDate?
        get() = parseDate(first("ORIGINALDATE") ?: first("RELEASEDATE") ?: first("DATE"))

    /** TagLib splits multi-valued fields for us, replacing the NUL separated string jaudiotagger produced. */
    val authors: List<String>?
        get() = properties["ARTIST"]?.filter { it.isNotBlank() }?.ifEmpty { null }

    val book: String?
        get() = first("ALBUM")

    val language: String?
        get() = first("LANGUAGE")

    val trackNr: Int?
        get() = first("TRACKNUMBER")?.substringBefore('/')?.trim()?.toIntOrNull()

    val narrator: String?
        get() = first("COMPOSER")

    /**
     * The series name is written to ID3v2's TIT1, which TagLib reports as WORK; MP4 files keep the
     * equivalent in the grouping atom, reported as GROUPING.
     */
    val series: String?
        get() = first("WORK") ?: first("GROUPING")

    val seriesIndex: Float?
        get() = first("CATALOGNUMBER")?.toFloatOrNull()

    private fun first(key: String): String? = properties[key]?.firstOrNull()?.ifBlank { null }

    private companion object {
        /** Tags carry either a full date or a bare year, depending on the format and tagger. */
        fun parseDate(value: String?): LocalDate? {
            val date = value?.trim()?.ifEmpty { null } ?: return null
            runCatching { return LocalDate.parse(date.take(10)) }
            return date.take(4).toIntOrNull()?.let { LocalDate.of(it, 1, 1) }
        }
    }
}
