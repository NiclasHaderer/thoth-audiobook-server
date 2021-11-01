package io.github.huiibuh.audible.api

import io.ktor.client.*
import io.ktor.http.*
import org.jsoup.nodes.Document

internal class BookHandler(
    client: HttpClient?,
    url: Url?,
    document: Document?,
) : AudibleHandler(client, url, document) {
    companion object {
        fun fromUrl(client: HttpClient, host: String, bookASIN: String): BookHandler {
            val url = URLBuilder(protocol = URLProtocol.HTTPS, host = host, encodedPath = "/pd/$bookASIN")
            url.parameters.append("ipRedirectOverride", "true")
            return BookHandler(client, url.build(), null)
        }

        fun fromDocument(document: Document): BookHandler {
            return BookHandler(null, null, document)
        }
    }

    // TODO change type
    override suspend fun execute(): Any {
        TODO("implement book extractor")
    }
}
