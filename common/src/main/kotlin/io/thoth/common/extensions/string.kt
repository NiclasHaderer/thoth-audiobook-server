package io.thoth.common.extensions

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.util.*

private val client = HttpClient()

suspend fun imageFromString(url: String): ByteArray {
    return if (url.matches("^data://".toRegex())) {
        decodeDataURL(url)
    } else {
        client.get(url).readBytes()
    }
}

internal fun decodeDataURL(dataUrl: String): ByteArray {
    val contentStartIndex: Int = dataUrl.indexOf(",") + 1
    val data = dataUrl.substring(contentStartIndex)
    return Base64.getDecoder().decode(data)
}

suspend fun String.uriToFile(): ByteArray = imageFromString(this)


fun String.replaceAll(values: List<Regex>, newValue: String) : String {
    var result = this
    values.forEach { result = result.replace(it, newValue) }
    return result
}

