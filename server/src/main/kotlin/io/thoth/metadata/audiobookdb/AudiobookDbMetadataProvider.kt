package io.thoth.metadata.audiobookdb

import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.thoth.metadata.MetadataHttpClient
import io.thoth.metadata.MetadataProvider
import io.thoth.metadata.ThrottleConfig
import io.thoth.metadata.appendOptional
import io.thoth.metadata.fullTextQuery
import io.thoth.metadata.httpsApiUrl
import io.thoth.metadata.responses.MetadataAuthorImpl
import io.thoth.metadata.responses.MetadataBookImpl
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import io.thoth.metadata.responses.MetadataSearchBookImpl
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeriesImpl
import io.thoth.metadata.toResultCount
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Duration

class AudiobookDbMetadataProvider(
    apiKey: String? = null,
) : MetadataProvider {
    private val http =
        MetadataHttpClient(
            "AudiobookDB",
            throttle = apiKey.let {
                val requestsPerSecond = if (apiKey == null) 5 else 10
                ThrottleConfig(requests = requestsPerSecond, window = Duration.ofSeconds(1))
            },
            defaultHeaders =
                Headers.build {
                    append(HttpHeaders.UserAgent, USER_AGENT)
                    if (apiKey != null) append(API_KEY_HEADER, apiKey)
                },
        )

    override val name = AUDIOBOOKDB_PROVIDER_NAME

    override val supportedRegions = MetadataRegion.entries

    override suspend fun search(
        region: MetadataRegion,
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?, // The book search cannot filter by language
        pageSize: MetadataSearchCount?,
    ): List<MetadataSearchBookImpl> {
        val query = fullTextQuery(keywords, title, author, narrator) ?: return emptyList()

        val parameters =
            Parameters.build {
                append("q", query)
                appendOptional("take", pageSize?.toResultCount(AUDIOBOOKDB_API_MAX_RESULTS)?.toString())
            }
        val url = apiUrl(listOf("books"), parameters)
        val items = http.getJson<List<AudiobookDbApiListBook>>(url) ?: return emptyList()
        return items.map { it.toMetadataSearchBook() }.filter { !it.title.isNullOrBlank() }
    }

    override suspend fun getAuthorByID(
        providerId: String,
        authorId: String,
        region: MetadataRegion,
    ): MetadataAuthorImpl? = http.getJson<AudiobookDbApiPerson>(apiUrl(listOf("people", authorId)))?.toMetadataAuthor()

    override suspend fun getBookByID(
        providerId: String,
        bookId: String,
        region: MetadataRegion,
    ): MetadataBookImpl? {
        val book = http.getJson<AudiobookDbApiBook>(apiUrl(listOf("books", bookId))) ?: return null
        return coroutineScope {
            val isbn =
                async {
                    book.releases.firstOrNull()?.let {
                        http.getJson<AudiobookDbApiReleaseDetail>(apiUrl(listOf("releases", it.id)))?.isbn
                    }
                }
            val rating =
                async {
                    http
                        .getJson<AudiobookDbApiRatings>(apiUrl(listOf("items", "book", bookId, "ratings")))
                        ?.chips
                        ?.firstNotNullOfOrNull { it.average }
                }
            book.toMetadataBook(isbn = isbn.await(), rating = rating.await())
        }
    }

    override suspend fun getSeriesByID(
        providerId: String,
        seriesId: String,
        region: MetadataRegion,
    ): MetadataSeriesImpl? = http.getJson<AudiobookDbApiSeries>(apiUrl(listOf("series", seriesId)))?.toMetadataSeries()

    private fun apiUrl(
        pathSegments: List<String>,
        parameters: Parameters = Parameters.Empty,
    ): Url = httpsApiUrl(AUDIOBOOKDB_API_HOST, listOf(AUDIOBOOKDB_API_PATH) + pathSegments, parameters)

    companion object {
        private const val AUDIOBOOKDB_API_HOST = "audiobookdb.org"
        private const val AUDIOBOOKDB_API_PATH = "api"

        private const val AUDIOBOOKDB_API_MAX_RESULTS = 100

        private const val API_KEY_HEADER = "X-API-Key"

        private const val USER_AGENT = "curl/7.68.0"
    }
}
