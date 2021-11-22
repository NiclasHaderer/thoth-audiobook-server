package audible.client

import audible.models.AudibleSearchAmount
import audible.models.AudibleSearchAuthor
import audible.models.AudibleSearchLanguage
import audible.models.AudibleSearchResult
import audible.models.AudibleSearchSeries
import io.ktor.client.*
import io.ktor.http.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.text.SimpleDateFormat
import java.util.*

@Suppress("EXPERIMENTAL_API_USAGE_FUTURE_ERROR")
internal class SearchHandler(
    client: HttpClient?,
    url: Url,
    document: Document?,
) : AudibleHandler(client, url, document) {

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

            return SearchHandler(client, url.build(), null)
        }

        fun fromDocument(document: Document, url: Url): SearchHandler {
            return SearchHandler(null, url, document)
        }
    }

    override suspend fun execute(): List<AudibleSearchResult> {
        val document = this.getDocument()
        val searchResultItems = getSearchItems(document)
        return extractSearchInfo(searchResultItems)
    }

    private fun getSearchItems(document: Document): Elements {
        return document.select(".productListItem")
    }

    private fun extractSearchInfo(elementList: Elements): List<AudibleSearchResult> {
        return elementList.map {
            object : AudibleSearchResult {
                override val author = extractAuthorInfo(it)
                override val title = extractTitle(it)
                override val link = extractLink(it)
                override val series = extractSeriesInfo(it)
                override val image = extractImageUrl(it)
                override val language = extractLanguage(it)
                override val releaseDate = extractReleaseDate(it)
                override val asin = idFromURL(this.link)
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

    private fun extractAuthorInfo(element: Element): AudibleSearchAuthor? {
        val authorLink = element.selectFirst(".authorLabel a") ?: return null
        return object : AudibleSearchAuthor {
            override val link = authorLink.absUrl("href")
            override val name = authorLink.text()
            override val asin = idFromURL(this.link)
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

    private fun extractSeriesInfo(element: Element): AudibleSearchSeries? {
        val seriesElement: Element = element.selectFirst(".seriesLabel") ?: return null
        val seriesNameElement = seriesElement.selectFirst("a") ?: return null

        var seriesIndex = seriesElement.selectFirst("span")?.text() ?: return null
        seriesIndex = seriesIndex.split(",").last().trim()
        seriesIndex = seriesIndex.filter { it.isDigit() }

        return object : AudibleSearchSeries {
            override val link = seriesNameElement.absUrl("href")
            override val name = seriesNameElement.text()
            override val index = seriesIndex.toFloat()
            override val asin = idFromURL(this.link)
        }

    }
}







