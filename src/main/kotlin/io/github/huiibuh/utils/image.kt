package io.github.huiibuh.utils

import io.ktor.client.*
import io.ktor.client.request.*
import java.util.*

private val client = HttpClient()

suspend fun imageFromString(url: String): ByteArray {
    return if (url.matches("^data://".toRegex())) {
        decodeDataURL(url)
    } else {
        client.get(url)
    }
}

internal fun decodeDataURL(dataUrl: String): ByteArray {
    val contentStartIndex: Int = dataUrl.indexOf(",") + 1
    val data = dataUrl.substring(contentStartIndex)
    return Base64.getDecoder().decode(data)
}
