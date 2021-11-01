package io.github.huiibuh.audible.api

import io.github.huiibuh.audible.models.AudibleSeries
import io.github.huiibuh.audible.models.AudibleSearchResult
import io.ktor.client.*
import io.ktor.http.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

internal class SeriesHandler(
    client: HttpClient?,
    url: Url?,
    document: Document?,
) : AudibleHandler(client, url, document) {
    companion object {
        fun fromURL(client: HttpClient, host: String, seriesASIN: String): SeriesHandler {
            val url = URLBuilder(protocol = URLProtocol.HTTPS, host = host, encodedPath = "/series/$seriesASIN")
            url.parameters.append("ipRedirectOverride", "true")
            return SeriesHandler(client, url.build(), null)
        }

        fun fromDocument(document: Document): SeriesHandler {
            return SeriesHandler(null, null, document)
        }
    }

    override suspend fun execute(): AudibleSeries {
        val document = getDocument()
        val booksInSeries = getSeriesBooks(document)
        return object : AudibleSeries {
            override val link = url.toString()
            override val asin = idFromURL(this.link)
            override val name = getSeriesName(document)
            override val description = getSeriesDescription(document)
            override val amount = getBookCount(document)
            override val books = booksInSeries
        }
    }


    fun getSeriesName(element: Element): String? {
        val authorElement = element.selectFirst("h1.bc-heading") ?: return null
        return authorElement.text()
    }


    fun getSeriesDescription(element: Element): String? {
        val biographyElement = element.selectFirst(".series-summary-content") ?: return null
        return biographyElement.text()
    }

    fun getBookCount(element: Element): Int? {
        val imageElement = element.selectFirst(".num-books-in-series") ?: return null
        return imageElement.text().filter { it.isDigit() }.toInt()
    }

    suspend fun getSeriesBooks(document: Document): List<AudibleSearchResult> {
        return SearchHandler.fromDocument(document).execute()
    }
}
