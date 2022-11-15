package io.thoth.metadata.audible.client

import io.thoth.metadata.AuthorMetadata
import io.thoth.metadata.audible.models.AudibleAuthorImpl
import io.thoth.metadata.audible.models.AudibleProviderWithIDMetadata
import org.jsoup.nodes.Element

suspend fun getAudibleAuthor(
    regions: AudibleRegions,
    imageSize: Int,
    authorAsin: String
): AuthorMetadata? {

    val document = getAudiblePage(regions, listOf("author", authorAsin)) ?: return null
    document.getElementById("product-list-a11y-skiplink-target") ?: return null
    return AudibleAuthorImpl(
        link = document.location().split("?").first(),
        id = AudibleProviderWithIDMetadata(audibleAsinFromLink(document.location())),
        name = getAuthorName(document),
        image = getAuthorImage(document, imageSize),
        biography = getAuthorBiography(document),
    )
}


private fun getAuthorName(element: Element) = element.selectFirst("h1.bc-heading")?.text()

private fun getAuthorBiography(element: Element) = element.selectFirst(".bc-expander span.bc-text")?.text()

private fun getAuthorImage(element: Element, imageSize: Int): String? {
    val imageElement = element.selectFirst("img.author-image-outline") ?: return null
    return toImageResAudible(imageElement.attr("src"), imageSize)
}
