package metadata.audible.client

import api.exceptions.APINotFound
import io.github.huiibuh.metadata.ProviderWithIDMetadata
import io.github.huiibuh.metadata.SearchResultMetadata
import io.github.huiibuh.metadata.SeriesMetadata
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

    override suspend fun execute(): SeriesMetadata {
        val document = getDocument()
        // Audible does not return 404 if a series is not valid, so...
        document.getElementById("product-list-a11y-skiplink-target")
            ?: throw APINotFound("Series could not be found")
        val booksInSeries = getSeriesBooks(document)
        val link = url.toString()
        return object : SeriesMetadata {
            override val link = link
            override val id = object : ProviderWithIDMetadata {
                override val provider = AUDIBLE_PROVIDER_NAME
                override val itemID = idFromURL(link)
            }
            override val name = getSeriesName(document)
            override val description = getSeriesDescription(document)
            override val amount = getBookCount(document)
            override val books = booksInSeries
        }
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

    private suspend fun getSeriesBooks(document: Document): List<SearchResultMetadata> {
        return SearchHandler.fromDocument(document, this.url).execute()
    }
}
