package io.thoth.metadata.audiobookdb

import io.thoth.metadata.htmlToText
import io.thoth.metadata.parseDateOrNull
import io.thoth.metadata.responses.MetadataAgentIDImpl
import io.thoth.metadata.responses.MetadataAuthorImpl
import io.thoth.metadata.responses.MetadataBookImpl
import io.thoth.metadata.responses.MetadataBookSeriesImpl
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchAuthorImpl
import io.thoth.metadata.responses.MetadataSearchBookImpl
import io.thoth.metadata.responses.MetadataSeriesImpl
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.OffsetDateTime

internal const val AUDIOBOOKDB_PROVIDER_NAME = "audiobookdb"

private const val AUTHOR_ROLE = "Author"
private const val NARRATOR_ROLE = "Narrator"

@Serializable
internal data class AudiobookDbApiIdName(
    val id: String,
    val name: String? = null,
) {
    fun toMetadataAuthor(): MetadataSearchAuthorImpl =
        MetadataSearchAuthorImpl(
            id = MetadataAgentIDImpl(AUDIOBOOKDB_PROVIDER_NAME, id),
            name = name,
            link = authorLink(id),
        )
}

@Serializable
internal data class AudiobookDbApiIdTitle(
    val id: String,
    val title: String? = null,
) {
    fun toMetadataBookSeries(index: Float?): MetadataBookSeriesImpl =
        MetadataBookSeriesImpl(
            id = MetadataAgentIDImpl(AUDIOBOOKDB_PROVIDER_NAME, id),
            title = title,
            link = seriesLink(id),
            index = index,
        )
}

@Serializable
internal data class AudiobookDbApiNamed(
    val name: String? = null,
)

@Serializable
internal data class AudiobookDbApiImage(
    val url: String? = null,
)

@Serializable
internal data class AudiobookDbApiCredit(
    val role: AudiobookDbApiNamed? = null,
    val person: AudiobookDbApiIdName? = null,
)

@Serializable
internal data class AudiobookDbApiListBook(
    val id: String,
    val title: String? = null,
    val image: AudiobookDbApiImage? = null,
    val authors: List<AudiobookDbApiIdName> = emptyList(),
    val series: AudiobookDbApiListSeries? = null,
) {
    fun toMetadataSearchBook(): MetadataSearchBookImpl =
        MetadataSearchBookImpl(
            id = MetadataAgentIDImpl(AUDIOBOOKDB_PROVIDER_NAME, id),
            title = title,
            link = bookLink(id),
            authors = authors.map { it.toMetadataAuthor() },
            series = listOfNotNull(series?.toMetadataBookSeries()),
            language = null,
            releaseDate = null,
            coverURL = image?.url,
            narrators = emptyList(),
        )
}

@Serializable
internal data class AudiobookDbApiListSeries(
    val ordinal: Int? = null,
    val series: AudiobookDbApiIdTitle? = null,
) {
    fun toMetadataBookSeries(): MetadataBookSeriesImpl? = series?.toMetadataBookSeries(ordinal?.toFloat())
}

@Serializable
internal data class AudiobookDbApiBook(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val originallyPublishedAt: String? = null,
    val originalLanguage: AudiobookDbApiNamed? = null,
    val coverImage: AudiobookDbApiImage? = null,
    val images: List<AudiobookDbApiImage> = emptyList(),
    val people: List<AudiobookDbApiCredit> = emptyList(),
    val releases: List<AudiobookDbApiRelease> = emptyList(),
    val series: List<AudiobookDbApiBookSeries> = emptyList(),
) {
    fun toMetadataBook(
        isbn: String?,
        rating: Float?,
    ): MetadataBookImpl {
        val release = releases.firstOrNull()
        return MetadataBookImpl(
            id = MetadataAgentIDImpl(AUDIOBOOKDB_PROVIDER_NAME, id),
            title = title,
            link = bookLink(id),
            authors = people.filter { it.role?.name == AUTHOR_ROLE }.mapNotNull { it.person?.toMetadataAuthor() },
            series = series.mapNotNull { it.toMetadataBookSeries() },
            releaseDate = parseDate(release?.releaseDate ?: originallyPublishedAt),
            coverURL = coverImage?.url ?: images.firstOrNull()?.url ?: release?.images?.firstOrNull()?.url,
            description = htmlToText(description),
            narrators = release?.people?.filter { it.role?.name == NARRATOR_ROLE }?.mapNotNull { it.person?.name }
                ?: emptyList(),
            providerRating = rating,
            publisher = release?.publisher?.name,
            language = MetadataLanguage.fromTag((release?.language ?: originalLanguage)?.name),
            isbn = isbn,
        )
    }
}

@Serializable
internal data class AudiobookDbApiBookSeries(
    val ordinal: String? = null,
    val series: AudiobookDbApiIdTitle? = null,
) {
    fun toMetadataBookSeries(): MetadataBookSeriesImpl? = series?.toMetadataBookSeries(ordinal?.toFloatOrNull())
}

@Serializable
internal data class AudiobookDbApiRelease(
    val id: String,
    val releaseDate: String? = null,
    val language: AudiobookDbApiNamed? = null,
    val publisher: AudiobookDbApiIdName? = null,
    val people: List<AudiobookDbApiCredit> = emptyList(),
    val images: List<AudiobookDbApiImage> = emptyList(),
)

@Serializable
internal data class AudiobookDbApiReleaseDetail(
    val isbn: String? = null,
)

@Serializable
internal data class AudiobookDbApiPerson(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    val birthDate: String? = null,
    val birthPlace: String? = null,
    val deathDate: String? = null,
    val links: List<String> = emptyList(),
    val images: List<AudiobookDbApiImage> = emptyList(),
) {
    fun toMetadataAuthor(): MetadataAuthorImpl =
        MetadataAuthorImpl(
            id = MetadataAgentIDImpl(AUDIOBOOKDB_PROVIDER_NAME, id),
            name = name,
            link = authorLink(id),
            imageURL = images.firstOrNull()?.url,
            biography = htmlToText(description),
            website = links.firstOrNull(),
            bornIn = birthPlace,
            birthDate = parseDate(birthDate),
            deathDate = parseDate(deathDate),
        )
}

@Serializable
internal data class AudiobookDbApiSeries(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val images: List<AudiobookDbApiImage> = emptyList(),
    val books: List<AudiobookDbApiSeriesEntry> = emptyList(),
) {
    fun toMetadataSeries(): MetadataSeriesImpl {
        val seriesRef = AudiobookDbApiIdTitle(id, title)
        val books =
            books
                .sortedBy { it.ordinal?.toFloatOrNull() ?: Float.MAX_VALUE }
                .mapNotNull { it.toMetadataSearchBook(seriesRef) }
        return MetadataSeriesImpl(
            id = MetadataAgentIDImpl(AUDIOBOOKDB_PROVIDER_NAME, id),
            title = title,
            link = seriesLink(id),
            description = htmlToText(description),
            totalBooks = books.size,
            primaryWorks = null,
            books = books,
            coverURL = images.firstOrNull()?.url ?: books.firstOrNull()?.coverURL,
            authors = null,
        )
    }
}

@Serializable
internal data class AudiobookDbApiSeriesEntry(
    val ordinal: String? = null,
    val book: AudiobookDbApiSeriesBook? = null,
) {
    fun toMetadataSearchBook(series: AudiobookDbApiIdTitle): MetadataSearchBookImpl? {
        val book = book ?: return null
        return MetadataSearchBookImpl(
            id = MetadataAgentIDImpl(AUDIOBOOKDB_PROVIDER_NAME, book.id),
            title = book.title,
            link = bookLink(book.id),
            authors = null,
            series = listOf(series.toMetadataBookSeries(ordinal?.toFloatOrNull())),
            language = null,
            releaseDate = null,
            coverURL = book.images.firstOrNull()?.url,
            narrators = emptyList(),
        )
    }
}

@Serializable
internal data class AudiobookDbApiSeriesBook(
    val id: String,
    val title: String? = null,
    val images: List<AudiobookDbApiImage> = emptyList(),
)

@Serializable
internal data class AudiobookDbApiRatings(
    val chips: List<AudiobookDbApiRatingChip> = emptyList(),
)

@Serializable
internal data class AudiobookDbApiRatingChip(
    val average: Float? = null,
)

private fun bookLink(bookId: String) = "https://audiobookdb.org/books/$bookId"

private fun authorLink(personId: String) = "https://audiobookdb.org/people/$personId"

private fun seriesLink(seriesId: String) = "https://audiobookdb.org/series/$seriesId"

private fun parseDate(date: String?): LocalDate? =
    parseDateOrNull("AudiobookDB", date) { OffsetDateTime.parse(it).toLocalDate() }
