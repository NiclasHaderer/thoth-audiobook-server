package io.thoth.metadata.audible.client.legacy

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory

internal abstract class AudibleHandler private constructor(
    private val client: HttpClient?,
    protected val url: Url,
    private val document: Document? = null,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    constructor(client: HttpClient, url: Url) : this(client, url, null)
    constructor(document: Document, url: Url) : this(null, url, document)

    abstract suspend fun execute(): Any?
    suspend fun getDocument(): Document? {
        if (this.document != null) return this.document

        val urlString = url.toString()
        val response = try {
            client!!.get(urlString) {
                headers {
                    append(
                        HttpHeaders.UserAgent,
                        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:93.0) Gecko/20100101 Firefox/93.0"
                    )
                    append(
                        HttpHeaders.Accept,
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
                    )
                    append(HttpHeaders.AcceptLanguage, "en-US;q=0.7,en;q=0.3")
                    append("Upgrade-Insecure-Requests", "1")
                    append("Sec-Fetch-Dest", "document")
                    append("Sec-Fetch-Mode", "navigate")
                    append("Sec-Fetch-Site", "none")
                    append("Sec-Fetch-User", "?1")
                }
            }
                .body<String>()
        } catch (e: ClientRequestException) {
            val message = e.localizedMessage.split("Text: ").firstOrNull() ?: ""
            val statusCode = e.response.status
            log.warn(
                """
                Audible crawler error
                Status: $statusCode
                Message: $message
            """.trimIndent()
            )

            return null
        }
        return Jsoup.parse(response, urlString)
    }

    protected fun idFromURL(link: String?): String {
        if (link == null) return ""
        return Url(link).encodedPath.split("/").last()
    }

    protected fun changeImageResolution(url: String, resolution: Int): String {
        var modifiedURL = url.replace(Regex("_SX\\d{2,4}_CR0"), "_SX${resolution}_CR0")
        modifiedURL = modifiedURL.replace(Regex(",0,.*"), ",0,0,0__.jpg")
        return modifiedURL
    }
}