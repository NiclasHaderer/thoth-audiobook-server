package io.thoth.metadata.audible.client

import io.thoth.common.extensions.replaceAll
import io.thoth.metadata.audible.models.AudibleProviderWithIDMetadata
import io.thoth.metadata.audible.models.AudibleRegions
import io.thoth.metadata.audible.models.getValue
import io.thoth.metadata.responses.MetadataBookImpl
import io.thoth.metadata.responses.MetadataSearchAuthorImpl
import io.thoth.metadata.responses.MetadataSearchSeriesImpl
import org.json.JSONArray
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.time.format.DateTimeFormatter

suspend fun getAudibleBook(
    region: AudibleRegions, asin: String
): MetadataBookImpl? {
    val document = getAudiblePage(region, listOf("pd", asin)) ?: return null

    return MetadataBookImpl(
        link = document.location().split("?").first(),
        id = AudibleProviderWithIDMetadata(audibleAsinFromLink(document.location())),
        description = getDescription(document),
        title = extractTitle(document, region),
        image = extractImageUrl(document),
        author = extractAuthorInfo(document),
        series = extractSeriesInfo(document),
        narrator = extractNarrator(document),
        date = getPublishedDate(document)
    )
}


private fun getPublishedDate(document: Document): LocalDate? {
    val jsonString = document.select("body script[type='application/ld+json']").firstOrNull()?.data() ?: return null
    return try {
        val jsonArray = JSONArray(jsonString)
        val datePublished = jsonArray.getJSONObject(0)?.get("datePublished") ?: return null

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        LocalDate.parse(datePublished.toString(), formatter)
    } catch (e: Exception) {
        null
    }
}

private fun extractNarrator(document: Document) = document.selectFirst(".narratorLabel a")?.text()

private fun extractAuthorInfo(document: Document): MetadataSearchAuthorImpl? {
    val authorLink = document.selectFirst(".authorLabel a") ?: return null
    val link = authorLink.absUrl("href").split("?").first()
    return MetadataSearchAuthorImpl(
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

private fun extractSeriesInfo(element: Element): MetadataSearchSeriesImpl? {
    val seriesElement: Element = element.selectFirst(".seriesLabel") ?: return null
    val seriesNameElement = seriesElement.selectFirst("a") ?: return null

    var seriesIndex = seriesElement.text().split(",").last().trim()
    seriesIndex = seriesIndex.filter { it.isDigit() }
    val link = seriesNameElement.absUrl("href").split("?").first()

    return MetadataSearchSeriesImpl(
        link = link,
        name = seriesNameElement.text(),
        index = seriesIndex.toFloatOrNull(),
        id = AudibleProviderWithIDMetadata(audibleAsinFromLink(link))
    )

}
