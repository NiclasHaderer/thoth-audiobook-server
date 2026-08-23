package io.thoth.metadata.libby.client

import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.thoth.metadata.appendOptional
import io.thoth.metadata.libby.models.LibbyAgentId
import io.thoth.metadata.libby.models.LibbyApiMedia
import io.thoth.metadata.libby.models.LibbyApiSearchResponse
import io.thoth.metadata.libby.models.LibbyApiSeriesItem
import io.thoth.metadata.libby.models.LibbyApiSeriesResponse
import io.thoth.metadata.responses.MetadataAuthorImpl
import io.thoth.metadata.responses.MetadataBookImpl
import io.thoth.metadata.responses.MetadataSearchBookImpl
import io.thoth.metadata.responses.MetadataSeriesImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val LIBBY_API_HOST = "thunder.api.overdrive.com"
private const val LIBBY_API_VERSION = "v2"

/** Amount of title IDs the bulk endpoint accepts in a single request. */
private const val LIBBY_API_TITLE_BATCH_SIZE = 50

internal const val LIBBY_API_MAX_RESULTS = 50

private val json = Json { ignoreUnknownKeys = true }

internal suspend fun getLibbySearchResult(
    libraryKey: String,
    imageSize: Int,
    keywords: String? = null,
    title: String? = null,
    author: String? = null,
    narrator: String? = null,
    language: String? = null,
    pageSize: Int? = null,
): List<MetadataSearchBookImpl> {
    // The catalog only offers a full text search, so all the lookup terms end up in one query
    val query = listOfNotNull(keywords, title, author, narrator).joinToString(" ").trim()
    if (query.isEmpty()) return emptyList()

    val parameters =
        Parameters.build {
            append("query", query)
            append("mediaTypes", LIBBY_AUDIOBOOK_TYPE)
            appendOptional("language", language)
            appendOptional("perPage", pageSize?.toString())
        }
    val url = libbyApiUrl(listOf("libraries", libraryKey, "media"), parameters)
    val items = getApi<LibbyApiSearchResponse>(url)?.items ?: return emptyList()
    return items.map { it.toMetadataSearchBook(libraryKey, imageSize) }
}

internal suspend fun getLibbyBook(
    libraryKey: String,
    imageSize: Int,
    titleId: String,
): MetadataBookImpl? = getApi<LibbyApiMedia>(libbyApiUrl(listOf("media", titleId)))?.toMetadataBook(libraryKey, imageSize)

/** Libby has no author entity, so the name has to be picked off a title the creator appears on. */
internal suspend fun getLibbyAuthor(
    libraryKey: String,
    creatorId: String,
): MetadataAuthorImpl? {
    val parameters =
        Parameters.build {
            append("creatorId", creatorId)
            append("perPage", "1")
        }
    val url = libbyApiUrl(listOf("libraries", libraryKey, "media"), parameters)
    val creator =
        getApi<LibbyApiSearchResponse>(url)
            ?.items
            ?.firstNotNullOfOrNull { item -> item.creators.firstOrNull { it.id?.toString() == creatorId } }
            ?: return null
    return MetadataAuthorImpl(
        id = LibbyAgentId(creatorId),
        name = creator.name,
        link = libbyAuthorLink(libraryKey, creatorId),
        imageURL = null,
        biography = null,
        website = null,
        bornIn = null,
        birthDate = null,
        deathDate = null,
    )
}

internal suspend fun getLibbySeries(
    libraryKey: String,
    imageSize: Int,
    seriesId: String,
): MetadataSeriesImpl? {
    val url = libbyApiUrl(listOf("libraries", libraryKey, "series", seriesId))
    val series = getApi<LibbyApiSeriesResponse>(url) ?: return null

    val orderedItems = series.items.orderedByReadingOrder()
    val mediaById = getLibbyMediaBulk(orderedItems.mapNotNull { it.id }).associateBy { it.id }
    // A series lists every edition and language of its books, so keep the most popular audiobook per reading order
    val books =
        orderedItems
            .mapNotNull { item -> mediaById[item.id]?.takeIf { it.type?.id == LIBBY_AUDIOBOOK_TYPE }?.let { item to it } }
            .distinctBy { (item, _) -> item.readingOrder ?: item.id }
            .map { (_, media) -> media.toMetadataSearchBook(libraryKey, imageSize) }

    return MetadataSeriesImpl(
        id = LibbyAgentId(seriesId),
        title = series.name,
        link = libbySeriesLink(libraryKey, seriesId),
        description = null,
        totalBooks = books.size,
        primaryWorks = null,
        books = books,
        coverURL = books.firstOrNull()?.coverURL,
        authors = books.firstOrNull()?.authors?.mapNotNull { it.name } ?: emptyList(),
    )
}

private fun List<LibbyApiSeriesItem>.orderedByReadingOrder(): List<LibbyApiSeriesItem> =
    sortedWith(
        compareBy(
            { it.readingOrder?.toFloatOrNull() ?: Float.MAX_VALUE },
            { it.rank ?: Int.MAX_VALUE },
        ),
    )

private suspend fun getLibbyMediaBulk(titleIds: List<String>): List<LibbyApiMedia> =
    coroutineScope {
        titleIds
            .chunked(LIBBY_API_TITLE_BATCH_SIZE)
            .map { chunk ->
                async {
                    val parameters = Parameters.build { append("titleIds", chunk.joinToString(",")) }
                    getApi<List<LibbyApiMedia>>(libbyApiUrl(listOf("media", "bulk"), parameters)) ?: emptyList()
                }
            }.awaitAll()
            .flatten()
    }

private fun libbyApiUrl(
    pathSegments: List<String>,
    parameters: Parameters = Parameters.Empty,
): Url =
    URLBuilder(
        protocol = URLProtocol.HTTPS,
        host = LIBBY_API_HOST,
        pathSegments = listOf(LIBBY_API_VERSION) + pathSegments,
        parameters = parameters,
    ).build()

private suspend inline fun <reified T> getApi(url: Url): T? {
    val body = fetchLibby(url) ?: return null

    return try {
        json.decodeFromString<T>(body)
    } catch (e: SerializationException) {
        throw LibbyUnavailableException("Could not deserialize the Libby API response of $url", e)
    }
}
