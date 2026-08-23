package io.thoth.metadata.libby.client

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
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
            socketTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
        }
    }

/** Libby could not be reached or answered something unusable. Distinct from Libby not having the requested item. */
class LibbyUnavailableException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/** Returns null if Libby does not have the requested item, and throws if the request itself did not work out. */
internal suspend fun fetchLibby(url: Url): String? {
    val response =
        try {
            client.get(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw LibbyUnavailableException("Libby request to $url failed", e)
        }

    if (response.status == HttpStatusCode.NotFound) {
        log.debug { "Libby has nothing at $url" }
        return null
    }
    if (!response.status.isSuccess()) {
        throw LibbyUnavailableException("Libby request to $url returned ${response.status}")
    }

    return try {
        response.bodyAsText()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw LibbyUnavailableException("Reading the Libby response of $url failed", e)
    }
}
