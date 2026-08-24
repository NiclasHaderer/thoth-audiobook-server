package io.thoth.metadata

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.Duration
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_LOGGED_BODY = 500

/** A provider could not be reached or answered something unusable. Distinct from it not having the requested item. */
class MetadataProviderUnavailableException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/** At most [requests] requests are started within any [window]. */
data class ThrottleConfig(
    val requests: Int,
    val window: Duration,
)

internal class MetadataHttpClient(
    val providerName: String,
    throttle: ThrottleConfig? = null,
    private val defaultHeaders: Headers = Headers.Empty,
) {
    private val throttle = throttle?.let { RequestThrottle(it) }

    private val client =
        HttpClient {
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                // Some providers answer a request they do not like by leaving the connection open without a body
                socketTimeoutMillis = 15_000
                requestTimeoutMillis = 30_000
            }
        }
    private val log = logger {}

    val json = Json { ignoreUnknownKeys = true }

    /** Returns null if the provider does not have the requested item, and throws if the request itself failed. */
    suspend fun fetch(
        url: Url,
        extraHeaders: Headers = Headers.Empty,
    ): String? {
        throttle?.awaitSlot()
        val response =
            try {
                client.get(url) {
                    headers {
                        appendAll(defaultHeaders)
                        appendAll(extraHeaders)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.debug(e) { "$providerName request to $url failed" }
                throw MetadataProviderUnavailableException("$providerName request to $url failed", e)
            }

        if (response.status == HttpStatusCode.NotFound) {
            log.debug { "$providerName has nothing at $url" }
            return null
        }
        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.bodyAsText() }.getOrNull()
            log.debug { "$providerName request to $url returned ${response.status}: ${errorBody?.take(MAX_LOGGED_BODY)}" }
            throw MetadataProviderUnavailableException("$providerName request to $url returned ${response.status}")
        }

        return try {
            response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw MetadataProviderUnavailableException("Reading the $providerName response of $url failed", e)
        }
    }

    /** An answer which is not the expected JSON is a failure and not an empty result. */
    suspend inline fun <reified T> getJson(url: Url): T? {
        val body = fetch(url) ?: return null
        return try {
            json.decodeFromString<T>(body)
        } catch (e: SerializationException) {
            throw MetadataProviderUnavailableException(
                "Could not deserialize the $providerName API response of $url",
                e
            )
        }
    }

    private class RequestThrottle(
        private val config: ThrottleConfig,
    ) {
        private val mutex = Mutex()
        private val startTimes = ArrayDeque<Long>()

        suspend fun awaitSlot() {
            while (true) {
                val waitFor =
                    mutex.withLock {
                        val now = System.currentTimeMillis()
                        while (startTimes.isNotEmpty() && now - startTimes.first() >= config.window.toMillis()) {
                            startTimes.removeFirst()
                        }
                        if (startTimes.size < config.requests) {
                            startTimes.addLast(now)
                            return
                        }
                        startTimes.first() + config.window.toMillis() - now
                    }
                delay(waitFor.milliseconds)
            }
        }
    }
}
