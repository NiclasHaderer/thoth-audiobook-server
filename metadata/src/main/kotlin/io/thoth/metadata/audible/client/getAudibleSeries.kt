package io.thoth.metadata.audible.client

import io.thoth.metadata.audible.models.AudibleProviderWithIDMetadata
import io.thoth.metadata.audible.models.AudibleSearchSeriesImpl
import io.thoth.metadata.audible.models.AudibleSeriesImpl
import org.jsoup.nodes.Element

suspend fun getAudibleSeries(
    region: AudibleRegions, asin: String
): AudibleSeriesImpl? {
    val document = getAudiblePage(region, listOf("series", asin)) ?: return null
    // Audible does not return 404 if a series is not valid, so...
    document.getElementById("product-list-a11y-skiplink-target") ?: return null

    val seriesBooks = getAudibleSearchResult(document, region)
    return AudibleSeriesImpl(
        link = document.location().split("?").first(),
        id = AudibleProviderWithIDMetadata(audibleAsinFromLink(document.location())),
        name = getSeriesName(document),
        description = getSeriesDescription(document),
        amount = getBookCount(document),
        books = seriesBooks,
        author = seriesBooks.firstOrNull()?.author?.name,
        image = null
    ).also {
        it.books?.forEachIndexed { index, book ->
            book.series = AudibleSearchSeriesImpl(
                id = it.id,
                name = it.name,
                index = index + 1f,
                link = it.link
            )
        }
    }
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
