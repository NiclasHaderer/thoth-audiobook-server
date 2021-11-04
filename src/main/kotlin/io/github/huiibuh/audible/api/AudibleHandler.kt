package io.github.huiibuh.audible.api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

internal abstract class AudibleHandler(
    private val client: HttpClient?,
    protected val url: Url,
    private val document: Document? = null,
) {

    init {
        if (this.document == null && this.client == null) {
            throw Exception("you have to set the document, or client and url")
        }
        if (this.document != null && this.client != null) {
            throw Exception("you cannot set a client and a document")
        }
    }

    abstract suspend fun execute(): Any
    suspend fun getDocument(): Document {
        if (this.document != null) return this.document

        val response: HttpStatement = client!!.get(url) {
            headers {
                append(
                    HttpHeaders.UserAgent,
                    "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:93.0) Gecko/20100101 Firefox/93.0"
                )
                append(
                    HttpHeaders.Accept,
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
                )
                append(HttpHeaders.AcceptLanguage, "en-US,de-DE;q=0.7,en;q=0.3")
                append("Upgrade-Insecure-Requests", "1")
                append("Sec-Fetch-Dest", "document")
                append("Sec-Fetch-Mode", "navigate")
                append("Sec-Fetch-Site", "none")
                append("Sec-Fetch-User", "?1")
                append("Pragma", "no-cache")
                append("Cache-Control", "no-cache")
            }
        }
        val body = response.receive<String>()
        return Jsoup.parse(body, this.url.toString())
    }

    fun idFromURL(link: String?): String {
        if (link == null) return ""
        return Url(link).encodedPath.split("/").last()
    }

    fun changeImageResolution(url: String, resolutio: Int = 500): String {
        var modifiedURL = url.replace(Regex("_SX\\d{2,4}_CR0"), "_SX${resolutio}_CR0")
        modifiedURL = modifiedURL.replace(Regex(",0,.*"), ",0,$resolutio,${resolutio}__.jpg")
        return modifiedURL
    }
}
