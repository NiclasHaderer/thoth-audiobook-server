package io.github.huiibuh.metadata.impl.audible.client

import io.github.huiibuh.metadata.BookMetadata
import io.github.huiibuh.metadata.ProviderWithIDMetadata
import io.github.huiibuh.metadata.SearchAuthorMetadata
import io.github.huiibuh.metadata.SearchSeriesMetadata
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

    override suspend fun execute(): BookMetadata {
        val document = getDocument()
        val link = url.toString()
        return object : BookMetadata {
            override val link = link
            override val id = object : ProviderWithIDMetadata {
                override val provider = AUDIBLE_PROVIDER_NAME
                override val itemID = idFromURL(link)
            }
            override val description = getDescription(document)
            override val title = extractTitle(document)
            override val image = extractImageUrl(document)
            override val author = extractAuthorInfo(document)
            override val series = extractSeriesInfo(document)

        }
    }

    private fun extractAuthorInfo(document: Document): SearchAuthorMetadata? {
        val authorLink = document.selectFirst(".authorLabel a") ?: return null
        val link = authorLink.absUrl("href")
        return object : SearchAuthorMetadata {
            override val link = link
            override val name = authorLink.text()
            override val id = object : ProviderWithIDMetadata {
                override val provider = AUDIBLE_PROVIDER_NAME
                override val itemID = idFromURL(link)
            }
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

    private fun extractSeriesInfo(element: Element): SearchSeriesMetadata? {
        val seriesElement: Element = element.selectFirst(".seriesLabel") ?: return null
        val seriesNameElement = seriesElement.selectFirst("a") ?: return null

        var seriesIndex = seriesElement.text().split(",").last().trim()
        seriesIndex = seriesIndex.filter { it.isDigit() }
        val link = seriesNameElement.absUrl("href")

        return object : SearchSeriesMetadata {
            override val link = link
            override val name = seriesNameElement.text()
            override val index = seriesIndex.toFloat()
            override val id = object : ProviderWithIDMetadata {
                override val provider = AUDIBLE_PROVIDER_NAME
                override val itemID = idFromURL(link)
            }
        }

    }
}
