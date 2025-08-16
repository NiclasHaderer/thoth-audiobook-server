package io.thoth.server.common.extensions

import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.runBlocking
import java.util.Base64
import java.util.UUID

private val client = HttpClient()

private suspend fun imageFromString(url: String): ByteArray =
    if (url.matches("^data://".toRegex())) {
        decodeDataURL(url)
    } else {
        client.get(url).readBytes()
    }

private fun decodeDataURL(dataUrl: String): ByteArray {
    val contentStartIndex: Int = dataUrl.indexOf(",") + 1
    val data = dataUrl.substring(contentStartIndex)
    return Base64.getDecoder().decode(data)
}

fun String.syncUriToFile(): ByteArray = runBlocking { imageFromString(this@syncUriToFile) }

suspend fun String.uriToFile(): ByteArray = imageFromString(this@uriToFile)

fun String.replaceAll(
    values: List<Regex>,
    newValue: String,
): String {
    var result = this
    values.forEach { result = result.replace(it, newValue) }
    return result
}

fun String.isUUID(): Boolean {
    val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
    return this.matches(uuidRegex)
}

fun String.toCron(): Cron {
    val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
    val parser = CronParser(cronDefinition)
    return parser.parse(this)
}

fun String.asUUID(): UUID = UUID.fromString(this)
