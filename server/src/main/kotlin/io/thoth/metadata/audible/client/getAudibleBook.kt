package io.thoth.metadata.audible.client

import io.thoth.metadata.audible.models.AudibleAgentId
import io.thoth.metadata.audible.models.AudibleRegions
import io.thoth.metadata.audible.models.getValue
import io.thoth.metadata.replaceAll
import io.thoth.metadata.responses.MetadataBookImpl
import org.json.JSONArray
import org.jsoup.nodes.Document
import java.time.LocalDate
import java.time.format.DateTimeFormatter

suspend fun getAudibleBook(
    region: AudibleRegions,
    asin: String,
): MetadataBookImpl? {
    val document = getAudiblePage(region, listOf("pd", asin)) ?: return null

    return MetadataBookImpl(
        link = document.location().split("?").first(),
        id = AudibleAgentId(audibleAsinFromLink(document.location())),
        description = getDescription(document),
        title = extractTitle(document, region),
        coverURL = extractImageUrl(document),
        authors = extractAuthorInfo(document),
        series = extractBookSeriesInfo(document),
        narrator = extractNarrator(document),
        releaseDate = getPublishedDate(document),
        publisher = null,
        providerRating = getBookRating(document),
        language = null,
        isbn = null,
    )
}

fun getBookRating(document: Document): Float? {
    val rating = document.selectFirst(".ratingsLabel > [aria-hidden]")?.text() ?: return null
    return rating.replace(",", ".").toFloatOrNull()
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

private fun getDescription(document: Document) = document.selectFirst(".productPublisherSummary span")?.text()

private fun extractImageUrl(document: Document) = document.selectFirst(".hero-content img.bc-pub-block")?.attr("src")

private fun extractTitle(
    document: Document,
    region: AudibleRegions,
): String? {
    val title = document.selectFirst("h1.bc-heading")?.text() ?: return null
    return title.replaceAll(region.getValue().titleReplacers, "")
}
