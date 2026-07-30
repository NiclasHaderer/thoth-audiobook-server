package io.thoth.metadata.audible.client

import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.thoth.metadata.appendOptional
import io.thoth.metadata.audible.models.AudibleAgentId
import io.thoth.metadata.audible.models.AudibleApiProduct
import io.thoth.metadata.audible.models.AudibleApiProductResponse
import io.thoth.metadata.audible.models.AudibleApiProductsResponse
import io.thoth.metadata.audible.models.AudibleRegions
import io.thoth.metadata.responses.MetadataBookImpl
import io.thoth.metadata.responses.MetadataSearchBookImpl
import io.thoth.metadata.responses.MetadataSeriesImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val AUDIBLE_API_VERSION = "1.0"

/** Amount of ASINs the catalog endpoint accepts in a single batch request. */
private const val AUDIBLE_API_ASIN_BATCH_SIZE = 50

/** Largest num_results the catalog endpoint accepts, it answers anything above with a 400. */
internal const val AUDIBLE_API_MAX_RESULTS = 50

/** Content delivery type Audible uses for the placeholder product which represents a series. */
private const val AUDIBLE_SERIES_DELIVERY_TYPE = "BookSeries"

private val productResponseGroups =
    listOf(
        "contributors",
        "media",
        "product_attrs",
        "product_desc",
        "product_details",
        "product_extended_attrs",
        "rating",
        "series",
    )

private val json = Json { ignoreUnknownKeys = true }

internal suspend fun getAudibleBook(
    region: AudibleRegions,
    imageSize: Int,
    asin: String,
): MetadataBookImpl? {
    val product = getAudibleProduct(region, imageSize, asin) ?: return null
    // The ASIN of a series resolves to a placeholder product, which is not a book
    if (product.contentDeliveryType == AUDIBLE_SERIES_DELIVERY_TYPE) return null
    return product.toMetadataBook(region, imageSize)
}

internal suspend fun getAudibleSearchResult(
    region: AudibleRegions,
    imageSize: Int,
    keywords: String? = null,
    title: String? = null,
    author: String? = null,
    narrator: String? = null,
    language: String? = null,
    pageSize: Int? = null,
): List<MetadataSearchBookImpl> {
    val parameters =
        Parameters.build {
            appendOptional("keywords", keywords)
            appendOptional("title", title)
            appendOptional("author", author)
            appendOptional("narrator", narrator)
            // num_results is applied before the language filter below, so a filtered search has to over-fetch
            appendOptional("num_results", (if (language == null) pageSize else AUDIBLE_API_MAX_RESULTS)?.toString())
        }
    val url = audibleApiUrl(region, listOf("catalog", "products"), imageSize, parameters)
    val products = getApi<AudibleApiProductsResponse>(url)?.products ?: return emptyList()

    // A marketplace serves more than one language and the catalog endpoint cannot filter by it
    return products
        .filter { language == null || it.language.equals(language, ignoreCase = true) }
        .let { if (pageSize == null) it else it.take(pageSize) }
        .map { it.toMetadataSearchBook(region, imageSize) }
}

internal suspend fun getAudibleSeries(
    region: AudibleRegions,
    imageSize: Int,
    asin: String,
): MetadataSeriesImpl? {
    val series = getAudibleProduct(region, imageSize, asin, listOf("relationships")) ?: return null
    // Audible returns a regular product for every ASIN, so make sure this one actually is a series
    if (series.contentDeliveryType != AUDIBLE_SERIES_DELIVERY_TYPE) return null

    val bookAsins = series.seriesBookAsins()
    val books = getAudibleProducts(region, imageSize, bookAsins)
    // The batch endpoint does not retain the requested order
    val booksByAsin = books.associateBy { it.asin }
    val seriesBooks = bookAsins.mapNotNull { booksByAsin[it] }.map { it.toMetadataSearchBook(region, imageSize) }

    return MetadataSeriesImpl(
        id = AudibleAgentId(series.asin),
        title = series.title,
        link = audibleSeriesLink(region, series.asin),
        description = audibleHtmlToText(series.publisherSummary ?: series.merchandisingSummary),
        // The relationships are the authority on the length of the series, not the books which could be resolved
        totalBooks = bookAsins.size,
        // Audible sequences excerpts and box sets right along the regular books, so the primary works are unknown
        primaryWorks = null,
        books = seriesBooks,
        coverURL = null,
        authors =
            series.authors.mapNotNull { it.name }.ifEmpty {
                seriesBooks.firstOrNull()?.authors?.mapNotNull { it.name } ?: emptyList()
            },
    )
}

private suspend fun getAudibleProduct(
    region: AudibleRegions,
    imageSize: Int,
    asin: String,
    extraResponseGroups: List<String> = emptyList(),
): AudibleApiProduct? {
    val url = audibleApiUrl(region, listOf("catalog", "products", asin), imageSize, extraGroups = extraResponseGroups)
    val product = getApi<AudibleApiProductResponse>(url)?.product
    // Unknown ASINs are answered with a product which only contains the ASIN itself
    return product?.takeIf { it.title != null }
}

private suspend fun getAudibleProducts(
    region: AudibleRegions,
    imageSize: Int,
    asins: List<String>,
): List<AudibleApiProduct> =
    coroutineScope {
        asins
            .chunked(AUDIBLE_API_ASIN_BATCH_SIZE)
            .map { chunk ->
                async {
                    val parameters = Parameters.build { append("asins", chunk.joinToString(",")) }
                    val url = audibleApiUrl(region, listOf("catalog", "products"), imageSize, parameters)
                    getApi<AudibleApiProductsResponse>(url)?.products ?: emptyList()
                }
            }.awaitAll()
            .flatten()
    }

private fun audibleApiUrl(
    region: AudibleRegions,
    pathSegments: List<String>,
    imageSize: Int,
    parameters: Parameters = Parameters.Empty,
    extraGroups: List<String> = emptyList(),
): Url =
    URLBuilder(
        protocol = URLProtocol.HTTPS,
        host = region.apiHost,
        pathSegments = listOf(AUDIBLE_API_VERSION) + pathSegments,
        parameters = parameters,
    ).also {
        it.parameters.append("response_groups", (productResponseGroups + extraGroups).joinToString(","))
        it.parameters.append("image_sizes", imageSize.toString())
    }.build()

private suspend inline fun <reified T> getApi(url: Url): T? {
    val body = fetchAudible(url) ?: return null

    return try {
        json.decodeFromString<T>(body)
    } catch (e: SerializationException) {
        // Audible sometimes answers with an HTML error page instead of JSON, which is a failure and not an empty result
        throw AudibleUnavailableException("Could not deserialize the Audible API response of $url", e)
    }
}
