package io.thoth.metadata.libby.client

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.metadata.htmlToText
import io.thoth.metadata.libby.models.LibbyAgentId
import io.thoth.metadata.libby.models.LibbyApiCreator
import io.thoth.metadata.libby.models.LibbyApiDetailedSeries
import io.thoth.metadata.libby.models.LibbyApiMedia
import io.thoth.metadata.responses.MetadataBookImpl
import io.thoth.metadata.responses.MetadataBookSeriesImpl
import io.thoth.metadata.responses.MetadataSearchAuthorImpl
import io.thoth.metadata.responses.MetadataSearchBookImpl
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

private val log = logger {}

private const val AUTHOR_ROLE = "Author"
private const val NARRATOR_ROLE = "Narrator"

internal const val LIBBY_AUDIOBOOK_TYPE = "audiobook"

internal fun LibbyApiMedia.toMetadataBook(
    libraryKey: String,
    imageSize: Int,
): MetadataBookImpl =
    MetadataBookImpl(
        id = LibbyAgentId(id),
        title = title,
        link = libbyBookLink(id),
        authors = creators.filter { it.role == AUTHOR_ROLE }.mapNotNull { it.toMetadataAuthor(libraryKey) },
        series = listOfNotNull(detailedSeries?.toMetadataBookSeries(libraryKey)),
        releaseDate = parseLibbyDate(publishDate ?: estimatedReleaseDate),
        coverURL = coverURL(imageSize),
        description = htmlToText(description),
        narrators = creators.filter { it.role == NARRATOR_ROLE }.mapNotNull { it.name },
        providerRating = starRating,
        publisher = publisher?.name,
        language = languages.firstOrNull()?.name?.lowercase(),
        isbn = formats.firstNotNullOfOrNull { it.isbn },
    )

internal fun LibbyApiMedia.toMetadataSearchBook(
    libraryKey: String,
    imageSize: Int,
): MetadataSearchBookImpl =
    MetadataSearchBookImpl(
        id = LibbyAgentId(id),
        title = title,
        link = libbyBookLink(id),
        authors = creators.filter { it.role == AUTHOR_ROLE }.mapNotNull { it.toMetadataAuthor(libraryKey) },
        series = listOfNotNull(detailedSeries?.toMetadataBookSeries(libraryKey)),
        releaseDate = parseLibbyDate(publishDate ?: estimatedReleaseDate),
        coverURL = coverURL(imageSize),
        narrators = creators.filter { it.role == NARRATOR_ROLE }.mapNotNull { it.name },
        language = languages.firstOrNull()?.name?.lowercase(),
    )

private fun LibbyApiCreator.toMetadataAuthor(libraryKey: String): MetadataSearchAuthorImpl? {
    val creatorId = id ?: return null
    return MetadataSearchAuthorImpl(
        id = LibbyAgentId(creatorId.toString()),
        name = name,
        link = libbyAuthorLink(libraryKey, creatorId.toString()),
    )
}

private fun LibbyApiDetailedSeries.toMetadataBookSeries(libraryKey: String): MetadataBookSeriesImpl? {
    val id = seriesId ?: return null
    return MetadataBookSeriesImpl(
        id = LibbyAgentId(id.toString()),
        title = seriesName,
        link = libbySeriesLink(libraryKey, id.toString()),
        index = readingOrder?.toFloatOrNull(),
    )
}

private fun LibbyApiMedia.coverURL(imageSize: Int): String? {
    val renditions = covers.values.filter { it.href != null }
    val cover =
        renditions.filter { (it.width ?: 0) >= imageSize }.minByOrNull { it.width ?: Int.MAX_VALUE }
            ?: renditions.maxByOrNull { it.width ?: 0 }
    return cover?.href
}

internal fun libbyBookLink(titleId: String) = "https://share.libbyapp.com/title/$titleId"

internal fun libbySeriesLink(
    libraryKey: String,
    seriesId: String,
) = "https://libbyapp.com/search/$libraryKey/search/series-$seriesId/page-1"

internal fun libbyAuthorLink(
    libraryKey: String,
    creatorId: String,
) = "https://libbyapp.com/search/$libraryKey/search/creator-$creatorId/page-1"

private fun parseLibbyDate(date: String?): LocalDate? =
    date?.let {
        try {
            OffsetDateTime.parse(it).toLocalDate()
        } catch (e: DateTimeParseException) {
            log.warn(e) { "Libby answered with the unparsable date '$it'" }
            null
        }
    }
