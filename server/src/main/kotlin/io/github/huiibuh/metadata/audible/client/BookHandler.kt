package io.github.huiibuh.metadata.audible.client

import io.github.huiibuh.metadata.audible.models.AudibleBookImpl
import io.github.huiibuh.metadata.audible.models.AudibleProviderWithIDMetadata
import io.github.huiibuh.metadata.audible.models.AudibleSearchAuthorImpl
import io.github.huiibuh.metadata.audible.models.AudibleSearchSeriesImpl
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

    override suspend fun execute(): AudibleBookImpl? {
        val document = getDocument() ?: return null
        val link = url.toString()
        return AudibleBookImpl(
            link = link,
            id = AudibleProviderWithIDMetadata(idFromURL(link)),
            description = getDescription(document),
            title = extractTitle(document),
            image = extractImageUrl(document),
            author = extractAuthorInfo(document),
            series = extractSeriesInfo(document),
            narrator = extractNarrator(document),
            year = null
        )
    }

    private fun extractNarrator(document: Document): String? {
        return document.selectFirst(".narratorLabel a")?.text()
    }

    private fun extractAuthorInfo(document: Document): AudibleSearchAuthorImpl? {
        val authorLink = document.selectFirst(".authorLabel a") ?: return null
        val link = authorLink.absUrl("href")
        return AudibleSearchAuthorImpl(
            link = link,
            name = authorLink.text(),
            id = AudibleProviderWithIDMetadata(idFromURL(link))
        )
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

    private fun extractSeriesInfo(element: Element): AudibleSearchSeriesImpl? {
        val seriesElement: Element = element.selectFirst(".seriesLabel") ?: return null
        val seriesNameElement = seriesElement.selectFirst("a") ?: return null

        var seriesIndex = seriesElement.text().split(",").last().trim()
        seriesIndex = seriesIndex.filter { it.isDigit() }
        val link = seriesNameElement.absUrl("href")

        return AudibleSearchSeriesImpl(
            link = link,
            name = seriesNameElement.text(),
            index = seriesIndex.toFloatOrNull(),
            id = AudibleProviderWithIDMetadata(idFromURL(link))
        )

    }
}
