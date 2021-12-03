package audible.client

import audible.models.AudibleBook
import audible.models.AudibleSearchAuthor
import audible.models.AudibleSearchSeries
import io.ktor.client.*
import io.ktor.http.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

@Suppress("EXPERIMENTAL_API_USAGE_FUTURE_ERROR")
internal class BookHandler : AudibleHandler {
    companion object {
        fun fromUrl(client: HttpClient, host: String, bookASIN: String): BookHandler {
            val url = URLBuilder(protocol = URLProtocol.HTTPS, host = host, encodedPath = "/pd/$bookASIN")
            url.parameters.append("ipRedirectOverride", "true")
            return BookHandler(client, url.build())
        }

        fun fromDocument(document: Document, url: Url): BookHandler {
            return BookHandler(document, url)
        }
    }

    constructor(client: HttpClient, url: Url) : super(client, url)

    constructor(document: Document, url: Url) : super(document, url)

    override suspend fun execute(): AudibleBook {
        val document = getDocument()
        return object : AudibleBook {
            override val link = url.toString()
            override val asin = idFromURL(link)
            override val description = getDescription(document)
            override val title = extractTitle(document)
            override val image = extractImageUrl(document)
            override val author = extractAuthorInfo(document)
            override val series = extractSeriesInfo(document)

        }
    }

    private fun extractAuthorInfo(document: Document): AudibleSearchAuthor? {
        val authorLink = document.selectFirst(".authorLabel a") ?: return null
        return object : AudibleSearchAuthor {
            override val link = authorLink.absUrl("href")
            override val name = authorLink.text()
            override val asin = idFromURL(this.link)
        }
    }

    private fun getDescription(document: Document): String? {
        return document.selectFirst(".productPublisherSummary span")?.text() ?: return null
    }

    private fun extractImageUrl(document: Document): String? {
        return document.selectFirst(".hero-content img.bc-pub-block")?.attr("src") ?: return null
    }

    private fun extractTitle(document: Document): String? {
        val title = document.selectFirst("h1.bc-heading")?.text() ?: return null
        return title.replace(", Book .*".toRegex(), "").replace(" - Gesprochen .*".toRegex(), "")
    }

    private fun extractSeriesInfo(element: Element): AudibleSearchSeries? {
        val seriesElement: Element = element.selectFirst(".seriesLabel") ?: return null
        val seriesNameElement = seriesElement.selectFirst("a") ?: return null

        var seriesIndex = seriesElement.text().split(",").last().trim()
        seriesIndex = seriesIndex.filter { it.isDigit() }

        return object : AudibleSearchSeries {
            override val link = seriesNameElement.absUrl("href")
            override val name = seriesNameElement.text()
            override val index = seriesIndex.toFloat()
            override val asin = idFromURL(this.link)
        }

    }
}
