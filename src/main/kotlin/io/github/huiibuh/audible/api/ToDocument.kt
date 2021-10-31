package io.github.huiibuh.audible.api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document


suspend fun getDocumentFromUrl(url: String): Document {
    val client = HttpClient(CIO)
    val response: HttpStatement = client.get(url) {
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
    client.close()
    return Jsoup.parse(body)
}
