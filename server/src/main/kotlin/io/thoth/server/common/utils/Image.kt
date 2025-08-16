package io.thoth.server.common.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import java.util.Base64

private val client by lazy { HttpClient() }

suspend fun imgFromURL(url: String): ByteArray =
    if (url.matches("^data://".toRegex())) {
        decodeDataURL(url)
    } else {
        client.get(url).readBytes()
    }

fun decodeDataURL(dataUrl: String): ByteArray {
    val contentStartIndex: Int = dataUrl.indexOf(",") + 1
    val data = dataUrl.substring(contentStartIndex)
    return Base64.getDecoder().decode(data)
}
