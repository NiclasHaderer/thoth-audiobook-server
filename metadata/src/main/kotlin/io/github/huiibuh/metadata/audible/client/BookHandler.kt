package io.github.huiibuh.metadata.audible.client

import io.ktor.client.*
import io.ktor.http.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

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

    override suspend fun execute(): io.github.huiibuh.metadata.audible.models.AudibleBookImpl? {
        val document = getDocument() ?: return null
        val link = url.toString()
        return io.github.huiibuh.metadata.audible.models.AudibleBookImpl(
            link = link,
            id = io.github.huiibuh.metadata.audible.models.AudibleProviderWithIDMetadata(idFromURL(link)),
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

    private fun extractAuthorInfo(document: Document): io.github.huiibuh.metadata.audible.models.AudibleSearchAuthorImpl? {
        val authorLink = document.selectFirst(".authorLabel a") ?: return null
        val link = authorLink.absUrl("href")
        return io.github.huiibuh.metadata.audible.models.AudibleSearchAuthorImpl(
            link = link,
            name = authorLink.text(),
            id = io.github.huiibuh.metadata.audible.models.AudibleProviderWithIDMetadata(idFromURL(link))
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

    private fun extractSeriesInfo(element: Element): io.github.huiibuh.metadata.audible.models.AudibleSearchSeriesImpl? {
        val seriesElement: Element = element.selectFirst(".seriesLabel") ?: return null
        val seriesNameElement = seriesElement.selectFirst("a") ?: return null

        var seriesIndex = seriesElement.text().split(",").last().trim()
        seriesIndex = seriesIndex.filter { it.isDigit() }
        val link = seriesNameElement.absUrl("href")

        return io.github.huiibuh.metadata.audible.models.AudibleSearchSeriesImpl(
            link = link,
            name = seriesNameElement.text(),
            index = seriesIndex.toFloatOrNull(),
            id = io.github.huiibuh.metadata.audible.models.AudibleProviderWithIDMetadata(idFromURL(link))
        )

    }
}
