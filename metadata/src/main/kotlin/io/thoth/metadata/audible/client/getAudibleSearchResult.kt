package io.thoth.metadata.audible.client

import io.ktor.http.*
import io.thoth.common.extensions.appendOptional
import io.thoth.common.extensions.replaceAll
import io.thoth.common.extensions.saveSubList
import io.thoth.metadata.audible.models.*
import io.thoth.metadata.responses.MetadataBookSeriesImpl
import io.thoth.metadata.responses.MetadataSearchAuthorImpl
import io.thoth.metadata.responses.MetadataSearchBookImpl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.time.LocalDate
import java.time.format.DateTimeFormatter

suspend fun getAudibleSearchResult(
    regions: AudibleRegions,
    keywords: String? = null,
    title: String? = null,
    author: String? = null,
    narrator: String? = null,
    language: AudibleSearchLanguage? = null,
    pageSize: AudibleSearchAmount? = null,
): List<MetadataSearchBookImpl>? {
    val urlParams = Parameters.build {
        appendOptional("keywords", keywords)
        appendOptional("title", title)
        appendOptional("author_author", author)
        appendOptional("narrator", narrator)
        appendOptional("feature_six_browse-bin", language?.language?.toString())
        appendOptional("pageSize", pageSize?.size?.toString())
    }

    val document = getAudiblePage(regions, listOf("search"), urlParams) ?: return null
    return getAudibleSearchResult(document, regions)
}

fun getAudibleSearchResult(document: Document, regions: AudibleRegions): List<MetadataSearchBookImpl> {
    val searchResultItems = extractSearchResults(document)
    return searchResultItems.map {
        val link = extractLink(it)
        MetadataSearchBookImpl(
            author = extractAuthorInfo(it),
            title = extractTitle(it, regions),
            link = link,
            series = extractBookSeriesInfo(it),
            coverURL = extractImageUrl(it),
            releaseDate = extractReleaseDate(it, regions),
            id = AudibleProviderWithIDMetadata(audibleAsinFromLink(link)),
            narrator = extractNarrator(it),
            language = extractLanguage(it),
        )
    }
}


private fun extractSearchResults(document: Document): Elements = document.select(".productListItem")


private fun extractNarrator(element: Element): String? = element.selectFirst(".narratorLabel a")?.text()

private fun extractLanguage(element: Element): String? =
    element.selectFirst(".languageLabel > *")?.text()?.split(":")?.last()?.trim()


private fun extractReleaseDate(element: Element, regions: AudibleRegions): LocalDate? {
    var date = element.selectFirst(".releaseDateLabel > *")?.text() ?: return null
    date = date.split(" ").last()
    val regionsValue = regions.getValue()
    val formatter = DateTimeFormatter.ofPattern(regionsValue.datePattern)
    return LocalDate.parse(date, formatter)
}


private fun extractImageUrl(element: Element): String? {
    val imageElement = element.selectFirst("img") ?: return null

    var imageURL = imageElement.attr("data-lazy")
    if (imageURL == "") {
        imageURL = imageElement.attr("src")
    }
    return imageURL
}


private fun extractAuthorInfo(element: Element): MetadataSearchAuthorImpl? {
    val authorLink = element.selectFirst(".authorLabel a") ?: return null
    val link = authorLink.absUrl("href").split("?").first()
    return MetadataSearchAuthorImpl(
        link = link,
        name = authorLink.text(),
        id = AudibleProviderWithIDMetadata(audibleAsinFromLink(link)),
    )
}


private fun extractTitle(element: Element, regions: AudibleRegions): String? =
    element.selectFirst("h3 a")?.text()?.replaceAll(regions.getValue().titleReplacers, "")


private fun extractLink(element: Element): String? = element.selectFirst("h3 a")?.absUrl("href")?.split("?")?.first()


internal fun extractBookSeriesInfo(element: Element): List<MetadataBookSeriesImpl> {
    val seriesElement: Element = element.selectFirst(".seriesLabel") ?: return listOf()
    val seriesLinkElements = seriesElement.select("a")
    if (seriesLinkElements.isEmpty()) return listOf()

    val seriesIndexElements = seriesElement.textNodes().saveSubList(1)

    return seriesLinkElements.mapIndexed { index, it ->
        val seriesLink = it.absUrl("href").split("?").first()
        val seriesTitle = it.text()

        val seriesIndexText = seriesIndexElements.getOrNull(index)?.text()
        val floatRegex = Regex("[0-9]+(\\.[0-9]+)?")
        // Find the last number in the string
        val seriesIndex = floatRegex.findAll(seriesIndexText ?: "").lastOrNull()?.value?.toFloatOrNull()

        MetadataBookSeriesImpl(
            link = seriesLink,
            title = seriesTitle,
            id = AudibleProviderWithIDMetadata(audibleAsinFromLink(seriesLink)),
            index = seriesIndex
        )
    }
}
