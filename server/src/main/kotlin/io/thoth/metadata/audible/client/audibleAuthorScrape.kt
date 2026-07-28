package io.thoth.metadata.audible.client

import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.thoth.metadata.audible.models.AudibleAgentId
import io.thoth.metadata.audible.models.AudibleRegions
import io.thoth.metadata.audible.models.getValue
import io.thoth.metadata.responses.MetadataAuthorImpl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

private val browserHeaders =
    Headers.build {
        append(HttpHeaders.UserAgent, "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:93.0) Gecko/20100101 Firefox/93.0")
        append(
            HttpHeaders.Accept,
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        )
        append(HttpHeaders.AcceptLanguage, "en-US;q=0.7,en;q=0.3")
        append("Upgrade-Insecure-Requests", "1")
        append("Sec-Fetch-Dest", "document")
        append("Sec-Fetch-Mode", "navigate")
        append("Sec-Fetch-Site", "none")
        append("Sec-Fetch-User", "?1")
    }

/** Authors are the only entity the Audible API does not expose, so their data has to be scraped. */
suspend fun getAudibleAuthor(
    region: AudibleRegions,
    imageSize: Int,
    authorAsin: String,
): MetadataAuthorImpl? {
    val document = getAudiblePage(region, listOf("author", authorAsin)) ?: return null
    // Audible answers unknown authors with a page which does not contain a product list
    document.getElementById("product-list-a11y-skiplink-target") ?: return null
    return MetadataAuthorImpl(
        link = audibleAuthorLink(region, authorAsin),
        id = AudibleAgentId(authorAsin),
        name = getAuthorName(document),
        imageURL = getAuthorImage(document, imageSize),
        biography = getAuthorBiography(document),
        website = null,
        deathDate = null,
        birthDate = null,
        bornIn = null,
    )
}

internal suspend fun getAudiblePage(
    region: AudibleRegions,
    pathSegments: List<String>,
): Document? {
    val url =
        URLBuilder(
            protocol = URLProtocol.HTTPS,
            host = region.getValue().toHost(),
            pathSegments = pathSegments,
        ).also {
            it.parameters.append("ipRedirectOverride", "true")
            // A locale prefixed path is dropped by the redirect to the canonical page, this parameter survives it
            it.parameters.append("language", region.getValue().locale)
        }.build()

    val page = fetchAudible(url, browserHeaders) ?: return null
    return Jsoup.parse(page, url.toString())
}

private fun getAuthorName(element: Element) = element.selectFirst("h1.bc-heading")?.text()

private fun getAuthorBiography(element: Element) = element.selectFirst(".bc-expander span.bc-text")?.text()

private fun getAuthorImage(
    element: Element,
    imageSize: Int,
): String? {
    val imageElement = element.selectFirst("img.author-image-outline") ?: return null
    return toImageResAudible(imageElement.attr("src"), imageSize)
}

private fun toImageResAudible(
    url: String,
    resolution: Int,
): String = url.replace(Regex("_SX\\d{2,4}_CR0"), "_SX${resolution}_CR0").replace(Regex(",0,.*"), ",0,0,0__.jpg")
