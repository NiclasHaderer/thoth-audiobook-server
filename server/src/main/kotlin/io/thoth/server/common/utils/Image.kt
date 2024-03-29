package io.thoth.server.common.utils

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.util.*

private val client by lazy { HttpClient() }

suspend fun imgFromURL(url: String): ByteArray {
    return if (url.matches("^data://".toRegex())) {
        decodeDataURL(url)
    } else {
        client.get(url).readBytes()
    }
}

fun decodeDataURL(dataUrl: String): ByteArray {
    val contentStartIndex: Int = dataUrl.indexOf(",") + 1
    val data = dataUrl.substring(contentStartIndex)
    return Base64.getDecoder().decode(data)
}
