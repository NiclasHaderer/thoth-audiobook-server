package io.thoth.metadata.audible.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.thoth.metadata.audible.models.AudibleRegions
import io.thoth.metadata.audible.models.getValue
import mu.KotlinLogging.logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

private val defaultHeaders =
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
private val client = HttpClient()
private val log = logger {}

suspend fun getAudiblePage(
    region: AudibleRegions,
    pathSegments: List<String>,
    parameters: Parameters = Parameters.build {},
): Document? {

    val url =
        URLBuilder(
                protocol = URLProtocol.HTTPS,
                host = region.getValue().toHost(),
                pathSegments = pathSegments,
                parameters = parameters,
            )
            .also { it.parameters.append("ipRedirectOverride", "true") }

    val response =
        try {
            client.get(url.build()) { headers { appendAll(defaultHeaders) } }.body<String>()
        } catch (e: ClientRequestException) {
            val message = e.localizedMessage.split("Text: ").firstOrNull() ?: ""
            val statusCode = e.response.status
            log.error(e) {
                """
                Audible crawler error
                Status: $statusCode
                Message: $message
            """
                    .trimIndent()
            }
            return null
        }

    return Jsoup.parse(response, url.build().toString())
}
