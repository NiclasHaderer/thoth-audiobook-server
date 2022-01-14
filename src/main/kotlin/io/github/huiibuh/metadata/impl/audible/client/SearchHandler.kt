package io.github.huiibuh.metadata.impl.audible.client

import io.github.huiibuh.metadata.ProviderWithIDMetadata
import io.github.huiibuh.metadata.SearchAuthorMetadata
import io.github.huiibuh.metadata.SearchResultMetadata
import io.github.huiibuh.metadata.SearchSeriesMetadata
import io.github.huiibuh.metadata.impl.audible.models.AudibleSearchAmount
import io.github.huiibuh.metadata.impl.audible.models.AudibleSearchLanguage
import io.ktor.client.*
import io.ktor.http.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.text.SimpleDateFormat
import java.util.*

@Suppress("EXPERIMENTAL_API_USAGE_FUTURE_ERROR")
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

    override suspend fun execute(): List<SearchResultMetadata>? {
        val document = this.getDocument() ?: return null
        val searchResultItems = getSearchItems(document)
        return extractSearchInfo(searchResultItems)
    }

    private fun getSearchItems(document: Document): Elements {
        return document.select(".productListItem")
    }

    private fun extractSearchInfo(elementList: Elements): List<SearchResultMetadata> {
        return elementList.map {
            val link = extractLink(it)
            object : SearchResultMetadata {
                override val author = extractAuthorInfo(it)
                override val title = extractTitle(it)
                override val link = link
                override val series = extractSeriesInfo(it)
                override val image = extractImageUrl(it)
                override val language = extractLanguage(it)
                override val narrator = extractNarrator(it)
                override val releaseDate = extractReleaseDate(it)
                override val id = object : ProviderWithIDMetadata {
                    override val provider = AUDIBLE_PROVIDER_NAME
                    override val itemID = idFromURL(link)
                }
            }
        }
    }

    private fun extractNarrator(element: Element): SearchAuthorMetadata? {
        val narratorLink = element.selectFirst(".narratorLabel a") ?: return null
        val link = narratorLink.absUrl("href")
        return object : SearchAuthorMetadata {
            override val link = link
            override val name = narratorLink.text()
            override val id = object : ProviderWithIDMetadata {
                override val provider = AUDIBLE_PROVIDER_NAME
                override val itemID = idFromURL(link)
            }
        }
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

    private fun extractAuthorInfo(element: Element): SearchAuthorMetadata? {
        val authorLink = element.selectFirst(".authorLabel a") ?: return null
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

    private fun extractTitle(element: Element): String? {
        val titleLink = element.selectFirst("h3 a") ?: return null
        return titleLink.text()
    }

    private fun extractLink(element: Element): String? {
        val titleLink = element.selectFirst("h3 a") ?: return null
        return titleLink.absUrl("href")
    }

    private fun extractSeriesInfo(element: Element): SearchSeriesMetadata? {
        val seriesElement: Element = element.selectFirst(".seriesLabel") ?: return null
        val seriesNameElement = seriesElement.selectFirst("a") ?: return null

        var seriesIndex = seriesElement.selectFirst("span")?.text() ?: return null
        seriesIndex = seriesIndex.split(",").last().trim()
        seriesIndex = seriesIndex.filter { it.isDigit() }
        val link = seriesNameElement.absUrl("href")

        return object : SearchSeriesMetadata {
            override val link = link
            override val name = seriesNameElement.text()
            override val index = seriesIndex.toFloatOrNull()
            override val id = object : ProviderWithIDMetadata {
                override val provider = AUDIBLE_PROVIDER_NAME
                override val itemID = idFromURL(link)
            }
        }

    }
}







