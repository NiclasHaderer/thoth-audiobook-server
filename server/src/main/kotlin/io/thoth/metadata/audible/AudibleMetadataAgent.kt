package io.thoth.metadata.audible

import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.thoth.metadata.MetadataHttpClient
import io.thoth.metadata.SearchBasedMetadataAgent
import io.thoth.metadata.ThrottleConfig
import io.thoth.metadata.appendOptional
import io.thoth.metadata.fetchChunked
import io.thoth.metadata.htmlToText
import io.thoth.metadata.httpsApiUrl
import io.thoth.metadata.responses.MetadataAgentIDImpl
import io.thoth.metadata.responses.MetadataAuthorImpl
import io.thoth.metadata.responses.MetadataBookImpl
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import io.thoth.metadata.responses.MetadataSearchBookImpl
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeriesImpl
import io.thoth.metadata.toResultCount
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.time.Duration

class AudibleMetadataAgent(
    private val imageSize: Int = 500,
) : SearchBasedMetadataAgent() {
    private val http =
        MetadataHttpClient("Audible", throttle = ThrottleConfig(requests = 1, window = Duration.ofSeconds(3)))

    override val name = AUDIBLE_PROVIDER_NAME

    override val supportedRegions: List<MetadataRegion>
        get() = AudibleRegions.entries.map { it.region }

    override suspend fun search(
        region: MetadataRegion,
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<MetadataSearchBookImpl> {
        val audibleRegion = AudibleRegions.from(region)
        val languageName = language?.name?.lowercase()
        val resultCount = pageSize?.toResultCount(AUDIBLE_API_MAX_RESULTS)

        val parameters =
            Parameters.build {
                appendOptional("keywords", keywords)
                appendOptional("title", title)
                appendOptional("author", author)
                appendOptional("narrator", narrator)
                // num_results is applied before the language filter below, so a filtered search has to over-fetch
                appendOptional(
                    "num_results",
                    (
                        if (languageName ==
                            null
                        ) {
                            resultCount
                        } else {
                            AUDIBLE_API_MAX_RESULTS
                        }
                    )?.toString(),
                )
            }
        val url = apiUrl(audibleRegion, listOf("catalog", "products"), parameters)
        val products = http.getJson<AudibleApiProductsResponse>(url)?.products ?: return emptyList()

        // A marketplace serves more than one language and the catalog endpoint cannot filter by it
        return products
            .filter { languageName == null || it.language.equals(languageName, ignoreCase = true) }
            .let { if (resultCount == null) it else it.take(resultCount) }
            .map { it.toMetadataSearchBook(audibleRegion, imageSize) }
            .filter { !it.title.isNullOrBlank() }
    }

    override suspend fun getAuthorByID(
        providerId: String,
        authorId: String,
        region: MetadataRegion,
    ): MetadataAuthorImpl? = scrapeAuthor(AudibleRegions.from(region), authorId)

    override suspend fun getBookByID(
        providerId: String,
        bookId: String,
        region: MetadataRegion,
    ): MetadataBookImpl? {
        val audibleRegion = AudibleRegions.from(region)
        val product = getProduct(audibleRegion, bookId) ?: return null
        // The ASIN of a series resolves to a placeholder product, which is not a book
        if (product.contentDeliveryType == AUDIBLE_SERIES_DELIVERY_TYPE) return null
        return product.toMetadataBook(audibleRegion, imageSize)
    }

    override suspend fun getSeriesByID(
        providerId: String,
        seriesId: String,
        region: MetadataRegion,
    ): MetadataSeriesImpl? {
        val audibleRegion = AudibleRegions.from(region)
        val series = getProduct(audibleRegion, seriesId, listOf("relationships")) ?: return null
        // Audible returns a regular product for every ASIN, so make sure this one actually is a series
        if (series.contentDeliveryType != AUDIBLE_SERIES_DELIVERY_TYPE) return null

        val bookAsins = series.seriesBookAsins()
        val books = getProducts(audibleRegion, bookAsins)
        // The batch endpoint does not retain the requested order
        val booksByAsin = books.associateBy { it.asin }
        val seriesBooks =
            bookAsins.mapNotNull { booksByAsin[it] }.map { it.toMetadataSearchBook(audibleRegion, imageSize) }

        return MetadataSeriesImpl(
            id = MetadataAgentIDImpl(AUDIBLE_PROVIDER_NAME, series.asin),
            title = series.title,
            link = audibleSeriesLink(audibleRegion, series.asin),
            description =
                htmlToText(series.publisherSummary ?: series.merchandisingSummary)
                    ?: scrapeSeriesDescription(audibleRegion, seriesId),
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

    private suspend fun getProduct(
        region: AudibleRegions,
        asin: String,
        extraResponseGroups: List<String> = emptyList(),
    ): AudibleApiProduct? {
        val url = apiUrl(region, listOf("catalog", "products", asin), extraGroups = extraResponseGroups)
        val product = http.getJson<AudibleApiProductResponse>(url)?.product
        // Unknown ASINs are answered with a product which only contains the ASIN itself
        return product?.takeIf { it.title != null }
    }

    private suspend fun getProducts(
        region: AudibleRegions,
        asins: List<String>,
    ): List<AudibleApiProduct> =
        fetchChunked(asins, AUDIBLE_API_ASIN_BATCH_SIZE) { chunk ->
            val parameters = Parameters.build { append("asins", chunk.joinToString(",")) }
            val url = apiUrl(region, listOf("catalog", "products"), parameters)
            http.getJson<AudibleApiProductsResponse>(url)?.products ?: emptyList()
        }

    private fun apiUrl(
        region: AudibleRegions,
        pathSegments: List<String>,
        parameters: Parameters = Parameters.Empty,
        extraGroups: List<String> = emptyList(),
    ): Url =
        httpsApiUrl(
            region.apiHost,
            listOf(AUDIBLE_API_VERSION) + pathSegments,
            Parameters.build {
                appendAll(parameters)
                append("response_groups", (productResponseGroups + extraGroups).joinToString(","))
                append("image_sizes", imageSize.toString())
            },
        )

    /** The Audible API does not expose authors, so their data has to be scraped. */
    internal suspend fun scrapeAuthor(
        region: AudibleRegions,
        authorAsin: String,
    ): MetadataAuthorImpl? {
        val document = scrapePage(region, listOf("author", authorAsin)) ?: return null
        // Audible answers unknown authors with a page which does not contain a product list
        document.getElementById("product-list-a11y-skiplink-target") ?: return null
        return MetadataAuthorImpl(
            link = audibleAuthorLink(region, authorAsin),
            id = MetadataAgentIDImpl(AUDIBLE_PROVIDER_NAME, authorAsin),
            name = document.selectFirst("h1.bc-heading")?.text(),
            imageURL = getAuthorImage(document),
            biography = document.selectFirst(".bc-expander span.bc-text")?.text(),
            website = null,
            deathDate = null,
            birthDate = null,
            bornIn = null,
        )
    }

    /** The API answers series with empty summaries, so the description has to be scraped off the series page. */
    internal suspend fun scrapeSeriesDescription(
        region: AudibleRegions,
        seriesAsin: String,
    ): String? {
        val document = scrapePage(region, listOf("series", seriesAsin)) ?: return null
        return htmlToText(seriesSummary(document)?.html())
    }

    internal suspend fun scrapePage(
        region: AudibleRegions,
        pathSegments: List<String>,
    ): Document? {
        val url =
            URLBuilder(
                protocol = URLProtocol.HTTPS,
                host = region.host,
                pathSegments = pathSegments,
            ).also {
                it.parameters.append("ipRedirectOverride", "true")
                // A locale prefixed path is dropped by the redirect to the canonical page, this parameter survives it
                it.parameters.append("language", region.locale)
            }.build()

        val page = http.fetch(url, browserHeaders) ?: return null
        return Jsoup.parse(page, url.toString())
    }

    private fun getAuthorImage(document: Document): String? {
        val imageElement = document.selectFirst("img.author-image-outline") ?: return null
        return imageElement
            .attr("src")
            .replace(imageResolution, "_SX${imageSize}_CR0")
            .replace(imageSuffix, ",0,0,0__.jpg")
    }

    // Audible serves two layouts of the series page interchangeably, each marks the summary up differently
    private fun seriesSummary(document: Document): Element? =
        document.selectFirst(".series-summary-content")
            ?: document.selectFirst("#series-about adbl-text-block")?.also { it.select("[slot=title]").remove() }

    companion object {
        private const val AUDIBLE_API_VERSION = "1.0"

        private const val AUDIBLE_API_ASIN_BATCH_SIZE = 50

        private const val AUDIBLE_API_MAX_RESULTS = 50

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

        private val imageResolution = Regex("_SX\\d{2,4}_CR0")
        private val imageSuffix = Regex(",0,.*")

        private val browserHeaders =
            Headers.build {
                append(
                    HttpHeaders.UserAgent,
                    "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:93.0) Gecko/20100101 Firefox/93.0",
                )
                append(
                    HttpHeaders.Accept,
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                )
                append(HttpHeaders.AcceptLanguage, "en-US;q=0.7,en;q=0.3")
                append("Upgrade-Insecure-Requests", "1")
                append("Sec-Fetch-Dest", "document")
                append("Sec-Fetch-Mode", "navigate")
                append("Sec-Fetch-Site", "none")
                append("Sec-Fetch-User", "?1")
            }
    }
}
