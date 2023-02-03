package io.thoth.metadata.audible.client

import io.thoth.metadata.audible.models.AudibleProviderWithIDMetadata
import io.thoth.metadata.audible.models.AudibleRegions
import io.thoth.metadata.responses.MetadataSearchSeriesImpl
import io.thoth.metadata.responses.MetadataSeriesImpl
import org.jsoup.nodes.Element

suspend fun getAudibleSeries(
    region: AudibleRegions, asin: String
): MetadataSeriesImpl? {
    val document = getAudiblePage(region, listOf("series", asin)) ?: return null
    // Audible does not return 404 if a series is not valid, so...
    document.getElementById("product-list-a11y-skiplink-target") ?: return null

    val seriesID = AudibleProviderWithIDMetadata(audibleAsinFromLink(document.location()))
    val seriesName = getSeriesName(document)
    val seriesLink = document.location().split("?").first()
    val seriesBooks = getAudibleSearchResult(document, region).mapIndexed { index, it ->
        it.copy(
            series = MetadataSearchSeriesImpl(
                id = seriesID,
                name = seriesName,
                index = index + 1f,
                link = seriesLink
            )
        )
    }
    return MetadataSeriesImpl(
        link = seriesLink,
        id = seriesID,
        name = seriesName,
        description = getSeriesDescription(document),
        amount = getBookCount(document),
        books = seriesBooks,
        author = seriesBooks.firstOrNull()?.author?.name,
        image = null,
    )
}


private fun getSeriesName(element: Element): String? {
    val authorElement = element.selectFirst("h1.bc-heading") ?: return null
    return authorElement.text()
}

private fun getSeriesDescription(element: Element): String? {
    val biographyElement = element.selectFirst(".series-summary-content") ?: return null
    return biographyElement.text()
}

private fun getBookCount(element: Element): Int? {
    val imageElement = element.selectFirst(".num-books-in-series") ?: return null
    return imageElement.text().filter { it.isDigit() }.toIntOrNull()
}
