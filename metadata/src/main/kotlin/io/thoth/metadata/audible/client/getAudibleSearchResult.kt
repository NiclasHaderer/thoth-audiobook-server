package io.thoth.metadata.audible.client

import io.ktor.http.*
import io.thoth.common.extensions.appendOptional
import io.thoth.metadata.audible.models.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.text.SimpleDateFormat
import java.util.*

suspend fun getAudibleSearchResult(
    regions: AudibleRegions,
    keywords: String? = null,
    title: String? = null,
    author: String? = null,
    narrator: String? = null,
    language: AudibleSearchLanguage? = null,
    pageSize: AudibleSearchAmount? = null,
): List<AudibleSearchBookImpl>? {
    val urlParams = Parameters.build {
        appendOptional("keywords", keywords)
        appendOptional("title", title)
        appendOptional("author_author", author)
        appendOptional("narrator", narrator)
        appendOptional("feature_six_browse-bin", language?.language.toString())
        appendOptional("pageSize", pageSize?.size.toString())
    }

    val document = getAudiblePage(regions, listOf("search"), urlParams) ?: return null
    return getAudibleSearchResult(document)
}

fun getAudibleSearchResult(document: Document): List<AudibleSearchBookImpl> {
    val searchResultItems = extractSearchResults(document)
    return extractSearchInfo(searchResultItems)
}


private fun extractSearchResults(document: Document): Elements = document.select(".productListItem")


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
            id = AudibleProviderWithIDMetadata(audibleAsinFromLink(link))
        )
    }
}

private fun extractNarrator(element: Element): String? = element.selectFirst(".narratorLabel a")?.text()

private fun extractLanguage(element: Element): String? =
    element.selectFirst(".languageLabel > *")?.text()?.split(":")?.last()?.trim()


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


private fun extractImageUrl(element: Element): String? = element.selectFirst("img")?.attr("data-lazy")


private fun extractAuthorInfo(element: Element): AudibleSearchAuthorImpl? {
    val authorLink = element.selectFirst(".authorLabel a") ?: return null
    val link = authorLink.absUrl("href")
    return AudibleSearchAuthorImpl(
        link = link,
        name = authorLink.text(),
        id = AudibleProviderWithIDMetadata(audibleAsinFromLink(link)),
    )
}


private fun extractTitle(element: Element): String? = element.selectFirst("h3 a")?.text()


private fun extractLink(element: Element): String? = element.selectFirst("h3 a")?.absUrl("href")


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
        id = AudibleProviderWithIDMetadata(audibleAsinFromLink(link))
    )

}