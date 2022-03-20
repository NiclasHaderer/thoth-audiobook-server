package io.github.huiibuh.metadata.audible.client

import io.github.huiibuh.metadata.audible.models.AudibleProviderWithIDMetadata
import io.github.huiibuh.metadata.audible.models.AudibleSearchBookImpl
import io.github.huiibuh.metadata.audible.models.AudibleSeriesImpl
import io.ktor.client.*
import io.ktor.http.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

@Suppress("EXPERIMENTAL_API_USAGE_FUTURE_ERROR")
internal class SeriesHandler : AudibleHandler {

    constructor(client: HttpClient, url: Url) : super(client, url)

    constructor(document: Document, url: Url) : super(document, url)


    companion object {
        fun fromURL(client: HttpClient, host: String, seriesASIN: String): SeriesHandler {
            val url = URLBuilder(protocol = URLProtocol.HTTPS, host = host, encodedPath = "/series/$seriesASIN")
            url.parameters.append("ipRedirectOverride", "true")
            return SeriesHandler(client, url.build())
        }

        fun fromDocument(document: Document, url: Url): SeriesHandler {
            return SeriesHandler(document, url)
        }
    }

    override suspend fun execute(): AudibleSeriesImpl? {
        val document = getDocument() ?: return null
        // Audible does not return 404 if a series is not valid, so...
        document.getElementById("product-list-a11y-skiplink-target")
            ?: return null
        val link = url.toString()
        val seriesBooks = getSeriesBooks(document)
        return AudibleSeriesImpl(
            link = link,
            id = AudibleProviderWithIDMetadata(idFromURL(link)),
            name = getSeriesName(document),
            description = getSeriesDescription(document),
            amount = getBookCount(document),
            books = seriesBooks,
            author = seriesBooks?.first()?.author?.name
        )
    }

    private fun getSeriesName(element: Element): String? {
        val authorElement = element.selectFirst("h1.bc-heading") ?: return null
        return authorElement.text()
    }

    private fun getSeriesDescription(element: Element): String? {
        val biographyElement = element.selectFirst(".series-summary-content") ?: return null
        return biographyElement.text()
    }

    private fun getBookCount(element: Element): Int? {
        val imageElement = element.selectFirst(".num-books-in-series") ?: return null
        return imageElement.text().filter { it.isDigit() }.toIntOrNull()
    }

    private suspend fun getSeriesBooks(document: Document): List<AudibleSearchBookImpl>? {
        // Document is provided, so there can be no exception fetching it
        return SearchHandler.fromDocument(document, this.url).execute()
    }
}
