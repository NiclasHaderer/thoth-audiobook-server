package io.thoth.metadata.audible.client

import io.thoth.metadata.audible.models.AudibleProviderWithIDMetadata
import io.thoth.metadata.audible.models.AudibleRegions
import io.thoth.metadata.responses.MetadataAuthorImpl
import org.jsoup.nodes.Element

suspend fun getAudibleAuthor(
    regions: AudibleRegions,
    imageSize: Int,
    authorAsin: String
): MetadataAuthorImpl? {

  val document = getAudiblePage(regions, listOf("author", authorAsin)) ?: return null
  document.getElementById("product-list-a11y-skiplink-target") ?: return null
  return MetadataAuthorImpl(
      link = document.location().split("?").first(),
      id = AudibleProviderWithIDMetadata(audibleAsinFromLink(document.location())),
      name = getAuthorName(document),
      imageURL = getAuthorImage(document, imageSize),
      biography = getAuthorBiography(document),
      website = null,
      deathDate = null,
      birthDate = null,
      bornIn = null,
  )
}

private fun getAuthorName(element: Element) = element.selectFirst("h1.bc-heading")?.text()

private fun getAuthorBiography(element: Element) =
    element.selectFirst(".bc-expander span.bc-text")?.text()

private fun getAuthorImage(element: Element, imageSize: Int): String? {
  val imageElement = element.selectFirst("img.author-image-outline") ?: return null
  return toImageResAudible(imageElement.attr("src"), imageSize)
}
