package io.thoth.metadata.audible.client

import io.ktor.client.*
import io.ktor.http.*
import io.thoth.metadata.audible.models.AudibleAuthorImpl
import io.thoth.metadata.audible.models.AudibleProviderWithIDMetadata
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

internal class AuthorHandler : AudibleHandler {
    private val imageSize: Int

    constructor(client: HttpClient, url: Url, imageSize: Int) : super(client, url) {
        this.imageSize = imageSize
    }

    constructor(document: Document, url: Url, imageSize: Int) : super(document, url) {
        this.imageSize = imageSize
    }

    companion object {
        fun fromURL(client: HttpClient, host: String, authorASIN: String, imageSize: Int = 500): AuthorHandler {
            val url = URLBuilder(protocol = URLProtocol.HTTPS, host = host, encodedPath = "/author/$authorASIN")
            url.parameters.append("ipRedirectOverride", "true")
            return AuthorHandler(client, url.build(), imageSize)
        }

        fun fromDocument(document: Document, url: Url, imageSize: Int = 500): AuthorHandler {
            return AuthorHandler(document, url, imageSize)
        }
    }

    override suspend fun execute(): AudibleAuthorImpl? {
        val document = getDocument() ?: return null
        document.getElementById("product-list-a11y-skiplink-target") ?: return null
        val link = url.toString()
        return AudibleAuthorImpl(
            link = link,
            id = AudibleProviderWithIDMetadata(idFromURL(link)),
            name = getAuthorName(document),
            image = getAuthorImage(document),
            biography = getAuthorBiography(document),
        )
    }

    private fun getAuthorName(element: Element): String? {
        val authorElement = element.selectFirst("h1.bc-heading") ?: return null
        return authorElement.text()
    }

    private fun getAuthorBiography(element: Element): String? {
        val biographyElement = element.selectFirst(".bc-expander span.bc-text") ?: return null
        return biographyElement.text()
    }

    private fun getAuthorImage(element: Element): String? {
        val imageElement = element.selectFirst("img.author-image-outline") ?: return null
        return changeImageResolution(imageElement.attr("src"), imageSize)
    }
}
