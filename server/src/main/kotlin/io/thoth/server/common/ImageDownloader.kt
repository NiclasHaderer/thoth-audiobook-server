package io.thoth.server.common

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.net.URI
import java.util.Base64

private const val MAX_IMAGE_BYTES = 16 * 1024 * 1024
private const val MAX_REDIRECTS = 5

/**
 * Fetches cover art from a URL which is untrusted input pointing at an unknown host
 * * may only reach public addresses
 * * may not read more than [MAX_IMAGE_BYTES].
 */
class ImageDownloader(
    private val client: HttpClient =
        HttpClient {
            followRedirects = false
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 10_000
            }
        },
) {
    fun download(source: String?): ByteArray? {
        if (source == null) return null
        return if (source.startsWith("data:")) decodeDataUrl(source) else runBlocking { fetch(source) }
    }

    private fun decodeDataUrl(dataUrl: String): ByteArray {
        val separator = dataUrl.indexOf(',')
        if (separator < 0) throw ErrorResponse.userError("Image data URL has no payload")
        val header = dataUrl.substring(0, separator)
        if (!header.endsWith(";base64")) {
            throw ErrorResponse.userError("Only base64 encoded image data URLs are supported")
        }
        val payload = dataUrl.substring(separator + 1)
        // 4 base64 characters per 3 bytes, so this rejects oversized payloads before allocating them
        if (payload.length / 4L * 3L > MAX_IMAGE_BYTES) throw imageTooLarge()
        return try {
            Base64.getDecoder().decode(payload)
        } catch (_: IllegalArgumentException) {
            throw ErrorResponse.userError("Image data URL is not valid base64")
        }
    }

    private suspend fun fetch(rawUrl: String): ByteArray {
        var target = rawUrl
        repeat(MAX_REDIRECTS + 1) {
            when (val outcome = requestOnce(target)) {
                is Outcome.Body -> return outcome.bytes
                is Outcome.Redirect -> target = outcome.location
            }
        }
        throw ErrorResponse.userError("Image URL redirects too many times")
    }

    private suspend fun requestOnce(target: String): Outcome {
        val url = publicHttpUrl(target)
        return client.prepareGet(url).execute { response ->
            val location = response.headers[HttpHeaders.Location]
            when {
                response.status.isRedirect() && location != null ->
                    Outcome.Redirect(URI(url).resolve(location).toString())

                response.status != HttpStatusCode.OK ->
                    throw ErrorResponse.userError("Image URL returned ${response.status.value}")

                else -> {
                    val declared = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                    if (declared != null && declared > MAX_IMAGE_BYTES) throw imageTooLarge()
                    Outcome.Body(response.bodyAsChannel().readCapped())
                }
            }
        }
    }

    private sealed interface Outcome {
        class Body(
            val bytes: ByteArray,
        ) : Outcome

        class Redirect(
            val location: String,
        ) : Outcome
    }
}

private fun HttpStatusCode.isRedirect(): Boolean = value in 301..308 && value != 304 && value != 305

// A Content-Length can lie or be absent, so the cap is enforced on what actually arrives
private suspend fun io.ktor.utils.io.ByteReadChannel.readCapped(): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val read = readAvailable(buffer)
        if (read <= 0) break
        if (out.size() + read > MAX_IMAGE_BYTES) throw imageTooLarge()
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}

private fun imageTooLarge() =
    ErrorResponse.userError("Image is larger than ${MAX_IMAGE_BYTES / (1024 * 1024)} MiB")

// Rejects everything that is not an ordinary public http(s) endpoint
private fun publicHttpUrl(target: String): String {
    val uri =
        try {
            URI(target)
        } catch (_: Exception) {
            throw ErrorResponse.userError("Image URL is not a valid URL")
        }
    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") {
        throw ErrorResponse.userError("Image URL must be http or https")
    }
    val host = uri.host ?: throw ErrorResponse.userError("Image URL has no host")

    val addresses =
        try {
            InetAddress.getAllByName(host)
        } catch (_: Exception) {
            throw ErrorResponse.userError("Image URL host cannot be resolved")
        }
    if (addresses.any { it.isPrivate() }) {
        throw ErrorResponse.userError("Image URL must point at a public address")
    }
    return uri.toString()
}

private fun InetAddress.isPrivate(): Boolean {
    if (isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress || isSiteLocalAddress || isMulticastAddress) {
        return true
    }
    val bytes = address
    return when (bytes.size) {
        // 100.64.0.0/10 carrier grade NAT, and 0.0.0.0/8 "this network"
        4 -> (bytes[0].toInt() and 0xFF == 100 && (bytes[1].toInt() and 0xC0) == 64) || bytes[0].toInt() == 0
        // fc00::/7 unique local addresses, which isSiteLocalAddress does not cover
        16 -> (bytes[0].toInt() and 0xFE) == 0xFC
        else -> false
    }
}
