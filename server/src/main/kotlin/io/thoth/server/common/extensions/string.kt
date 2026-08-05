package io.thoth.server.common.extensions

import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.runBlocking
import java.util.Base64

private val client =
    HttpClient {
        // Non-2xx must not end up stored as the image body
        expectSuccess = true
        // The download happens while a database transaction is open, so it may not hang forever
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
    }

private suspend fun imageFromString(url: String): ByteArray =
    if (url.startsWith("data:")) {
        decodeDataURL(url)
    } else {
        client.get(url).readRawBytes()
    }

private fun decodeDataURL(dataUrl: String): ByteArray {
    val contentStartIndex: Int = dataUrl.indexOf(",") + 1
    val data = dataUrl.substring(contentStartIndex)
    return Base64.getDecoder().decode(data)
}

fun String.syncUriToFile(): ByteArray = runBlocking { imageFromString(this@syncUriToFile) }

fun String.replaceAll(
    values: List<Regex>,
    newValue: String,
): String {
    var result = this
    values.forEach { result = result.replace(it, newValue) }
    return result
}

private val DIGIT_RUN = "\\d+".toRegex()

/** Orders strings the way a file browser does, so "Chapter 2" comes before "Chapter 10" instead of after it. */
val naturalOrder: Comparator<String> =
    Comparator { left, right ->
        val leftParts = left.splitOnDigits()
        val rightParts = right.splitOnDigits()
        leftParts
            .zip(rightParts) { a, b ->
                val numbers = a.toBigIntegerOrNull()?.let { x -> b.toBigIntegerOrNull()?.let { y -> x to y } }
                numbers?.first?.compareTo(numbers.second) ?: String.CASE_INSENSITIVE_ORDER.compare(a, b)
            }.firstOrNull { it != 0 } ?: leftParts.size.compareTo(rightParts.size)
    }

private fun String.splitOnDigits(): List<String> {
    val parts = mutableListOf<String>()
    var index = 0
    for (match in DIGIT_RUN.findAll(this)) {
        if (match.range.first > index) parts += substring(index, match.range.first)
        parts += match.value
        index = match.range.last + 1
    }
    if (index < length) parts += substring(index)
    return parts
}

private val UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()

fun String.isUUID(): Boolean = this.matches(UUID_REGEX)

fun String.toCron(): Cron {
    val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
    val parser = CronParser(cronDefinition)
    return parser.parse(this)
}
