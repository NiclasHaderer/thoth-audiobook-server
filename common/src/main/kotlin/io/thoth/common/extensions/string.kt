package io.thoth.common.extensions

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import java.util.*

private val client = HttpClient()

private suspend fun imageFromString(url: String): ByteArray {
    return if (url.matches("^data://".toRegex())) {
        decodeDataURL(url)
    } else {
        client.get(url).readBytes()
    }
}

private fun decodeDataURL(dataUrl: String): ByteArray {
    val contentStartIndex: Int = dataUrl.indexOf(",") + 1
    val data = dataUrl.substring(contentStartIndex)
    return Base64.getDecoder().decode(data)
}

fun String.syncUriToFile(): ByteArray = runBlocking { imageFromString(this@syncUriToFile) }
suspend fun String.uriToFile(): ByteArray = imageFromString(this@uriToFile)

fun String.replaceAll(values: List<Regex>, newValue: String): String {
    var result = this
    values.forEach { result = result.replace(it, newValue) }
    return result
}

fun String.isUUID(): Boolean {
    val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
    return this.matches(uuidRegex)
}