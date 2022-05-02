package io.thoth.metadata.audible.client

import io.ktor.client.*
import io.ktor.http.*
import io.thoth.metadata.audible.models.AudibleProviderWithIDMetadata
import io.thoth.metadata.audible.models.AudibleSearchAmount
import io.thoth.metadata.audible.models.AudibleSearchAuthorImpl
import io.thoth.metadata.audible.models.AudibleSearchBookImpl
import io.thoth.metadata.audible.models.AudibleSearchLanguage
import io.thoth.metadata.audible.models.AudibleSearchSeriesImpl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.text.SimpleDateFormat
import java.util.*

internal class SearchHandler : AudibleHandler {

    constructor(client: HttpClient, url: Url) : super(client, url)

    constructor(document: Document, url: Url) : super(document, url)

    companion object {
        fun fromURL(
            client: HttpClient,
            host: String,
            keywords: String? = null,
            title: String? = null,
            author: String? = null,
            narrator: String? = null,
            language: AudibleSearchLanguage? = null,
            pageSize: AudibleSearchAmount? = null,
        ): SearchHandler {
            val queryParams = ParametersBuilder()
            queryParams.append("ipRedirectOverride", "true")
            if (keywords != null) {
                queryParams.append("keywords", keywords)
            }
            if (title != null) {
                queryParams.append("title", title)
            }
            if (author != null) {
                queryParams.append("author_author", author)
            }
            if (narrator != null) {
                queryParams.append("narrator", narrator)
            }
            if (language != null) {
                queryParams.append("feature_six_browse-bin", language.language.toString())
            }
            if (pageSize != null) {
                queryParams.append("pageSize", pageSize.size.toString())
            }

            val url = URLBuilder(
                protocol = URLProtocol.HTTPS,
                parameters = queryParams,
                host = host,
                encodedPath = "/search"
            )

            return SearchHandler(client, url.build())
        }

        fun fromDocument(document: Document, url: Url): SearchHandler {
            return SearchHandler(document, url)
        }
    }

    override suspend fun execute(): List<AudibleSearchBookImpl>? {
        val document = this.getDocument() ?: return null
        val searchResultItems = getSearchItems(document)
        return extractSearchInfo(searchResultItems)
    }

    private fun getSearchItems(document: Document): Elements {
        return document.select(".productListItem")
    }

    private fun extractSearchInfo(elementList: Elements): List<AudibleSearchBookImpl> {
        return elementList.map {
            val link = extractLink(it)
            AudibleSearchBookImpl(
                author = extractAuthorInfo(it),
                title = extractTitle(it),
                link = link,
                series = extractSeriesInfo(it),
                image = extractImageUrl(it),
                language = extractLanguage(it),
                narrator = extractNarrator(it),
                releaseDate = extractReleaseDate(it),
                id = AudibleProviderWithIDMetadata(idFromURL(link))
            )
        }
    }

    private fun extractNarrator(element: Element): String? {
        val narratorLink = element.selectFirst(".narratorLabel a") ?: return null
        return narratorLink.text()
    }

    private fun extractLanguage(element: Element): String? {
        return element.selectFirst(".languageLabel > *")?.text()?.split(":")?.last()?.trim()
    }

    private fun extractReleaseDate(element: Element): Date? {
        var date = element.selectFirst(".releaseDateLabel > *")?.text() ?: return null
        date = date.split(" ").last()
        val parsedDate = try {
            val enPattern = SimpleDateFormat("MM-dd-yy")
            enPattern.parse(date)
        } catch (e: Exception) {
            val dePattern = SimpleDateFormat("dd.MM.yyyy")
            dePattern.parse(date)
        }
        return parsedDate
    }

    private fun extractImageUrl(element: Element): String? {
        return element.selectFirst("img")?.attr("data-lazy")

    }

    private fun extractAuthorInfo(element: Element): AudibleSearchAuthorImpl? {
        val authorLink = element.selectFirst(".authorLabel a") ?: return null
        val link = authorLink.absUrl("href")
        return AudibleSearchAuthorImpl(
            link = link,
            name = authorLink.text(),
            id = AudibleProviderWithIDMetadata(idFromURL(link)),
        )
    }

    private fun extractTitle(element: Element): String? {
        val titleLink = element.selectFirst("h3 a") ?: return null
        return titleLink.text()
    }

    private fun extractLink(element: Element): String? {
        val titleLink = element.selectFirst("h3 a") ?: return null
        return titleLink.absUrl("href")
    }

    private fun extractSeriesInfo(element: Element): AudibleSearchSeriesImpl? {
        val seriesElement: Element = element.selectFirst(".seriesLabel") ?: return null
        val seriesNameElement = seriesElement.selectFirst("a") ?: return null

        var seriesIndex = seriesElement.selectFirst("span")?.text() ?: return null
        seriesIndex = seriesIndex.split(",").last().trim()
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







