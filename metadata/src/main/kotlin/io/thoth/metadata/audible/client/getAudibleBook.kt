package io.thoth.metadata.audible.client

import io.thoth.common.extensions.replaceAll
import io.thoth.metadata.audible.models.AudibleBookImpl
import io.thoth.metadata.audible.models.AudibleProviderWithIDMetadata
import io.thoth.metadata.audible.models.AudibleSearchAuthorImpl
import io.thoth.metadata.audible.models.AudibleSearchSeriesImpl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

suspend fun getAudibleBook(
    region: AudibleRegions, asin: String
): AudibleBookImpl? {
    val document = getAudiblePage(region, listOf("pd", asin)) ?: return null

    return AudibleBookImpl(
        link = document.location(),
        id = AudibleProviderWithIDMetadata(audibleAsinFromLink(document.location())),
        description = getDescription(document),
        title = extractTitle(document, region),
        image = extractImageUrl(document),
        author = extractAuthorInfo(document),
        series = extractSeriesInfo(document),
        narrator = extractNarrator(document),
        year = null
    )
}


private fun extractNarrator(document: Document) = document.selectFirst(".narratorLabel a")?.text()

private fun extractAuthorInfo(document: Document): AudibleSearchAuthorImpl? {
    val authorLink = document.selectFirst(".authorLabel a") ?: return null
    val link = authorLink.absUrl("href")
    return AudibleSearchAuthorImpl(
        link = link,
        name = authorLink.text(),
        id = AudibleProviderWithIDMetadata(audibleAsinFromLink(link))
    )
}

private fun getDescription(document: Document) = document.selectFirst(".productPublisherSummary span")?.text()

private fun extractImageUrl(document: Document) = document.selectFirst(".hero-content img.bc-pub-block")?.attr("src")

private fun extractTitle(document: Document, region: AudibleRegions): String? {
    val title = document.selectFirst("h1.bc-heading")?.text() ?: return null
    return title.replaceAll(region.getValue().titleReplacers, "")
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
        id = AudibleProviderWithIDMetadata(audibleAsinFromLink(link))
    )

}