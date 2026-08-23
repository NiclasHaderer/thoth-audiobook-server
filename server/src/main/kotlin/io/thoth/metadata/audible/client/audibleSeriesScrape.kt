package io.thoth.metadata.audible.client

import io.thoth.metadata.htmlToText
import io.thoth.metadata.audible.models.AudibleRegions
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** The API answers series with empty summaries, so the description has to be scraped off the series page. */
internal suspend fun getAudibleSeriesDescription(
    region: AudibleRegions,
    seriesAsin: String,
): String? {
    val document = getAudiblePage(region, listOf("series", seriesAsin)) ?: return null
    return htmlToText(seriesSummary(document)?.html())
}

// Audible serves two layouts of the series page interchangeably, each marks the summary up differently
private fun seriesSummary(document: Document): Element? =
    document.selectFirst(".series-summary-content")
        ?: document.selectFirst("#series-about adbl-text-block")?.also { it.select("[slot=title]").remove() }
