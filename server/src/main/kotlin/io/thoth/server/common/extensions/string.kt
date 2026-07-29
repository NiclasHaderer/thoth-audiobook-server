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

private val UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()

fun String.isUUID(): Boolean = this.matches(UUID_REGEX)

fun String.toCron(): Cron {
    val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
    val parser = CronParser(cronDefinition)
    return parser.parse(this)
}
