package io.thoth.metadata.libby

import io.thoth.metadata.htmlToText
import io.thoth.metadata.parseDateOrNull
import io.thoth.metadata.responses.MetadataAgentIDImpl
import io.thoth.metadata.responses.MetadataBookImpl
import io.thoth.metadata.responses.MetadataBookSeriesImpl
import io.thoth.metadata.responses.MetadataSearchAuthorImpl
import io.thoth.metadata.responses.MetadataSearchBookImpl
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.OffsetDateTime

internal const val LIBBY_PROVIDER_NAME = "libby"

private const val AUTHOR_ROLE = "Author"
private const val NARRATOR_ROLE = "Narrator"

@Serializable
internal data class LibbyApiSearchResponse(
    val items: List<LibbyApiMedia> = emptyList(),
)

@Serializable
internal data class LibbyApiMedia(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val creators: List<LibbyApiCreator> = emptyList(),
    val languages: List<LibbyApiLanguage> = emptyList(),
    val publisher: LibbyApiPublisher? = null,
    val detailedSeries: LibbyApiDetailedSeries? = null,
    val covers: Map<String, LibbyApiCover> = emptyMap(),
    val formats: List<LibbyApiFormat> = emptyList(),
    val type: LibbyApiMediaType? = null,
    val starRating: Float? = null,
    val publishDate: String? = null,
    val estimatedReleaseDate: String? = null,
) {
    fun toMetadataBook(
        libraryKey: String,
        imageSize: Int,
    ): MetadataBookImpl =
        MetadataBookImpl(
            id = MetadataAgentIDImpl(LIBBY_PROVIDER_NAME, id),
            title = title,
            link = libbyBookLink(id),
            authors = creators.filter { it.role == AUTHOR_ROLE }.mapNotNull { it.toMetadataAuthor(libraryKey) },
            series = listOfNotNull(detailedSeries?.toMetadataBookSeries(libraryKey)),
            releaseDate = parseDate(publishDate ?: estimatedReleaseDate),
            coverURL = coverURL(imageSize),
            description = htmlToText(description),
            narrators = creators.filter { it.role == NARRATOR_ROLE }.mapNotNull { it.name },
            providerRating = starRating,
            publisher = publisher?.name,
            language = languages.firstOrNull()?.name?.lowercase(),
            isbn = formats.firstNotNullOfOrNull { it.isbn },
        )

    fun toMetadataSearchBook(
        libraryKey: String,
        imageSize: Int,
    ): MetadataSearchBookImpl =
        MetadataSearchBookImpl(
            id = MetadataAgentIDImpl(LIBBY_PROVIDER_NAME, id),
            title = title,
            link = libbyBookLink(id),
            authors = creators.filter { it.role == AUTHOR_ROLE }.mapNotNull { it.toMetadataAuthor(libraryKey) },
            series = listOfNotNull(detailedSeries?.toMetadataBookSeries(libraryKey)),
            releaseDate = parseDate(publishDate ?: estimatedReleaseDate),
            coverURL = coverURL(imageSize),
            narrators = creators.filter { it.role == NARRATOR_ROLE }.mapNotNull { it.name },
            language = languages.firstOrNull()?.name?.lowercase(),
        )

    private fun coverURL(imageSize: Int): String? {
        val renditions = covers.values.filter { it.href != null }
        val cover =
            renditions.filter { (it.width ?: 0) >= imageSize }.minByOrNull { it.width ?: Int.MAX_VALUE }
                ?: renditions.maxByOrNull { it.width ?: 0 }
        return cover?.href
    }
}

@Serializable
internal data class LibbyApiCreator(
    val id: Long? = null,
    val name: String? = null,
    val role: String? = null,
) {
    fun toMetadataAuthor(libraryKey: String): MetadataSearchAuthorImpl? {
        val creatorId = id ?: return null
        return MetadataSearchAuthorImpl(
            id = MetadataAgentIDImpl(LIBBY_PROVIDER_NAME, creatorId.toString()),
            name = name,
            link = libbyAuthorLink(libraryKey, creatorId.toString()),
        )
    }
}

@Serializable
internal data class LibbyApiLanguage(
    val id: String? = null,
    val name: String? = null,
)

@Serializable
internal data class LibbyApiPublisher(
    val name: String? = null,
)

@Serializable
internal data class LibbyApiDetailedSeries(
    val seriesId: Long? = null,
    val seriesName: String? = null,
    val readingOrder: String? = null,
) {
    fun toMetadataBookSeries(libraryKey: String): MetadataBookSeriesImpl? {
        val id = seriesId ?: return null
        return MetadataBookSeriesImpl(
            id = MetadataAgentIDImpl(LIBBY_PROVIDER_NAME, id.toString()),
            title = seriesName,
            link = libbySeriesLink(libraryKey, id.toString()),
            index = readingOrder?.toFloatOrNull(),
        )
    }
}

@Serializable
internal data class LibbyApiCover(
    val href: String? = null,
    val width: Int? = null,
)

@Serializable
internal data class LibbyApiFormat(
    val id: String? = null,
    val isbn: String? = null,
)

@Serializable
internal data class LibbyApiMediaType(
    val id: String? = null,
)

@Serializable
internal data class LibbyApiSeriesResponse(
    val id: Long? = null,
    val name: String? = null,
    val items: List<LibbyApiSeriesItem> = emptyList(),
)

@Serializable
internal data class LibbyApiSeriesItem(
    val id: String? = null,
    val title: String? = null,
    val readingOrder: String? = null,
    val rank: Int? = null,
)

internal fun libbyBookLink(titleId: String) = "https://share.libbyapp.com/title/$titleId"

internal fun libbySeriesLink(
    libraryKey: String,
    seriesId: String,
) = "https://libbyapp.com/search/$libraryKey/search/series-$seriesId/page-1"

internal fun libbyAuthorLink(
    libraryKey: String,
    creatorId: String,
) = "https://libbyapp.com/search/$libraryKey/search/creator-$creatorId/page-1"

private fun parseDate(date: String?): LocalDate? = parseDateOrNull("Libby", date) { OffsetDateTime.parse(it).toLocalDate() }
