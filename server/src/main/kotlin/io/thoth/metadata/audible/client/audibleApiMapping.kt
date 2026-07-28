package io.thoth.metadata.audible.client

import io.thoth.metadata.audible.models.AudibleAgentId
import io.thoth.metadata.audible.models.AudibleApiPerson
import io.thoth.metadata.audible.models.AudibleApiProduct
import io.thoth.metadata.audible.models.AudibleApiSeries
import io.thoth.metadata.audible.models.AudibleRegions
import io.thoth.metadata.audible.models.getValue
import io.thoth.metadata.replaceAll
import io.thoth.metadata.responses.MetadataBookImpl
import io.thoth.metadata.responses.MetadataBookSeriesImpl
import io.thoth.metadata.responses.MetadataSearchAuthorImpl
import io.thoth.metadata.responses.MetadataSearchBookImpl
import org.jsoup.parser.Parser
import java.time.LocalDate

private val htmlLineBreak = Regex("(?i)<br\\s*/?>|</p\\s*>")
private val htmlTag = Regex("<[^>]+>")
private val paddedNewline = Regex("[^\\S\n]*\n[^\\S\n]*")
private val repeatedNewline = Regex("\n{3,}")

internal fun AudibleApiProduct.toMetadataBook(
    region: AudibleRegions,
    imageSize: Int,
): MetadataBookImpl =
    MetadataBookImpl(
        id = AudibleAgentId(asin),
        title = cleanTitle(region),
        link = audibleBookLink(region, asin),
        authors = authors.mapNotNull { it.toMetadataAuthor(region) },
        series = series.mapNotNull { it.toMetadataBookSeries(region) },
        releaseDate = parseAudibleDate(releaseDate ?: issueDate),
        coverURL = coverURL(imageSize),
        description = audibleHtmlToText(publisherSummary ?: merchandisingSummary),
        narrator = narrators.mapNotNull { it.name }.ifEmpty { null }?.joinToString(", "),
        providerRating = rating?.overallDistribution?.averageRating,
        publisher = publisherName,
        language = language,
        isbn = isbn,
    )

internal fun AudibleApiProduct.toMetadataSearchBook(
    region: AudibleRegions,
    imageSize: Int,
): MetadataSearchBookImpl =
    MetadataSearchBookImpl(
        id = AudibleAgentId(asin),
        title = cleanTitle(region),
        link = audibleBookLink(region, asin),
        authors = authors.mapNotNull { it.toMetadataAuthor(region) },
        series = series.mapNotNull { it.toMetadataBookSeries(region) },
        releaseDate = parseAudibleDate(releaseDate ?: issueDate),
        coverURL = coverURL(imageSize),
        narrator = narrators.mapNotNull { it.name }.ifEmpty { null }?.joinToString(", "),
        language = language,
    )

private fun AudibleApiPerson.toMetadataAuthor(region: AudibleRegions): MetadataSearchAuthorImpl? {
    // Narrators and series placeholder products come without an ASIN, which makes them unusable as a referent
    val authorAsin = asin ?: return null
    return MetadataSearchAuthorImpl(
        id = AudibleAgentId(authorAsin),
        name = name,
        link = audibleAuthorLink(region, authorAsin),
    )
}

private fun AudibleApiSeries.toMetadataBookSeries(region: AudibleRegions): MetadataBookSeriesImpl? {
    val seriesAsin = asin ?: return null
    return MetadataBookSeriesImpl(
        id = AudibleAgentId(seriesAsin),
        title = title,
        link = audibleSeriesLink(region, seriesAsin),
        index = sequence?.toFloatOrNull(),
    )
}

/** ASINs of the books of a series, in the order Audible sequences them. */
internal fun AudibleApiProduct.seriesBookAsins(): List<String> =
    relationships
        .filter { it.relationshipToProduct == "child" && it.relationshipType == "series" }
        .sortedBy { it.sequence?.toFloatOrNull() ?: Float.MAX_VALUE }
        .mapNotNull { it.asin }
        .distinct()

private fun AudibleApiProduct.cleanTitle(region: AudibleRegions): String? =
    title?.replaceAll(region.getValue().titleReplacers, "")?.trim()

private fun AudibleApiProduct.coverURL(imageSize: Int): String? =
    productImages[imageSize.toString()] ?: productImages.values.firstOrNull()

internal fun audibleBookLink(
    region: AudibleRegions,
    asin: String,
) = "https://www.${region.getValue().toHost()}/pd/$asin"

internal fun audibleSeriesLink(
    region: AudibleRegions,
    asin: String,
) = "https://www.${region.getValue().toHost()}/series/$asin"

internal fun audibleAuthorLink(
    region: AudibleRegions,
    asin: String,
) = "https://www.${region.getValue().toHost()}/author/$asin"

/** Audible serves summaries as HTML fragments, while the metadata responses are plain text. */
internal fun audibleHtmlToText(html: String?): String? =
    html
        ?.replace(htmlLineBreak, "\n")
        ?.replace(htmlTag, "")
        ?.let { Parser.unescapeEntities(it, false) }
        ?.replace(paddedNewline, "\n")
        ?.replace(repeatedNewline, "\n\n")
        ?.trim()
        ?.ifEmpty { null }

private fun parseAudibleDate(date: String?): LocalDate? =
    date?.let {
        try {
            LocalDate.parse(it)
        } catch (e: Exception) {
            null
        }
    }
