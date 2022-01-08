package io.github.huiibuh.metadata.impl.audible.client

import io.github.huiibuh.api.exceptions.APINotFound
import io.github.huiibuh.metadata.AuthorMetadata
import io.github.huiibuh.metadata.ProviderWithIDMetadata
import io.ktor.client.*
import io.ktor.http.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

@Suppress("EXPERIMENTAL_API_USAGE_FUTURE_ERROR")
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

    override suspend fun execute(): AuthorMetadata {
        val document = getDocument()
        document.getElementById("product-list-a11y-skiplink-target")
            ?: throw APINotFound("Author could not be found")
        val link = url.toString()
        return object : AuthorMetadata {
            override val link = link
            override val id = object : ProviderWithIDMetadata {
                override val provider = AUDIBLE_PROVIDER_NAME
                override val itemID = idFromURL(link)
            }
            override val name = getAuthorName(document)
            override val image = getAuthorImage(document)
            override val biography = getAuthorBiography(document)
        }
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
