package io.thoth.metadata.audible.client

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.isSuccess
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

private val log = logger {}

private val client =
    HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            // Audible answers a request it does not like by leaving the connection open without sending a body
            socketTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
        }
    }

/** Audible could not be reached or answered something unusable. Distinct from Audible not having the requested item. */
class AudibleUnavailableException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/** Returns null if Audible does not have the requested item, and throws if the request itself did not work out. */
internal suspend fun fetchAudible(
    url: Url,
    extraHeaders: Headers = Headers.Empty,
): String? {
    val response =
        try {
            client.get(url) { headers { appendAll(extraHeaders) } }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw AudibleUnavailableException("Audible request to $url failed", e)
        }

    if (response.status == HttpStatusCode.NotFound) {
        log.debug { "Audible has nothing at $url" }
        return null
    }
    if (!response.status.isSuccess()) {
        throw AudibleUnavailableException("Audible request to $url returned ${response.status}")
    }

    return try {
        response.bodyAsText()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw AudibleUnavailableException("Reading the Audible response of $url failed", e)
    }
}
