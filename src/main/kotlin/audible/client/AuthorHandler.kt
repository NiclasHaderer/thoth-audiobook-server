package audible.client

import api.exceptions.APINotFound
import audible.models.AudibleAuthor
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

    override suspend fun execute(): AudibleAuthor {
        val document = getDocument()
        document.getElementById("product-list-a11y-skiplink-target")
            ?: throw APINotFound("Author could not be found")
        return object : AudibleAuthor {
            override val link = url.toString()
            override val asin = idFromURL(this.link)
            override val name = getAuthorName(document)
            override val image = getAuthorImage(document)
            override val biography = getAuthorBiography(document)
        }
    }

    fun getAuthorName(element: Element): String? {
        val authorElement = element.selectFirst("h1.bc-heading") ?: return null
        return authorElement.text()
    }

    fun getAuthorBiography(element: Element): String? {
        val biographyElement = element.selectFirst(".bc-expander span.bc-text") ?: return null
        return biographyElement.text()
    }

    fun getAuthorImage(element: Element): String? {
        val imageElement = element.selectFirst("img.author-image-outline") ?: return null
        return changeImageResolution(imageElement.attr("src"), imageSize)
    }
}
