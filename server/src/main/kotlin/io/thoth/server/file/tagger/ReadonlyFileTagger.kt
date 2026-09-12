package io.thoth.server.file.tagger

import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.server.common.extensions.canonicalString
import io.thoth.server.common.extensions.lastModifiedInstant
import io.thoth.taglib.TagLibFile
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import kotlin.io.path.nameWithoutExtension

class ReadonlyFileTagger(
    filePath: Path,
) {
    constructor(path: String) : this(Path.of(path))

    private val properties: Map<String, List<String>>

    val cover: ByteArray?
    val durationMs: Long
    val path: String = filePath.canonicalString()
    val lastModified: Instant = filePath.lastModifiedInstant()

    init {
        TagLibFile(filePath).use { file ->
            properties = file.properties()
            cover = file.pictures().firstOrNull()?.data
            durationMs = file.lengthInSeconds * 1000L
        }
    }

    val title: String
        get() = first("TITLE") ?: Path.of(path).nameWithoutExtension

    val description: String?
        get() = first("PODCASTDESC") ?: first("COMMENT")

    val date: LocalDate?
        get() = parseDate(first("ORIGINALDATE") ?: first("RELEASEDATE") ?: first("DATE"))

    /** TagLib splits multi-valued fields for us, replacing the NUL separated string jaudiotagger produced. */
    val authors: List<String>?
        get() = properties["ARTIST"]?.filter { it.isNotBlank() }?.ifEmpty { null }

    val book: String?
        get() = first("ALBUM")

    val genres: List<String>
        get() = properties["GENRE"].orEmpty().mapNotNull { it.trim().ifBlank { null } }.distinct()

    val language: MetadataLanguage?
        get() = MetadataLanguage.fromTag(first("LANGUAGE"))

    val trackNr: Int?
        get() = first("TRACKNUMBER")?.substringBefore('/')?.trim()?.toIntOrNull()

    val narrators: List<String>
        get() =
            properties["COMPOSER"]
                .orEmpty()
                .flatMap { it.split(",") }
                .mapNotNull { it.trim().ifBlank { null } }
                .distinct()

    val series: String?
        get() = first("WORK") ?: first("GROUPING") ?: first("SERIES")

    val seriesIndex: Float?
        get() = (first("CATALOGNUMBER") ?: first("PART"))?.toFloatOrNull()

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
