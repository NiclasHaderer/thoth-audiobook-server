package io.thoth.server.common

import io.ktor.http.ContentType
import java.io.ByteArrayInputStream
import java.net.URLConnection

private val WEBP = ContentType("image", "webp")
private val AVIF = ContentType("image", "avif")
private val BMP = ContentType("image", "bmp")

private val SUPPORTED_IMAGES =
    setOf(ContentType.Image.PNG, ContentType.Image.JPEG, ContentType.Image.GIF, WEBP, AVIF, BMP)

fun imageContentType(bytes: ByteArray): ContentType? {
    // The JDK sniffer only knows the classic formats. It reports webp as audio/x-wav, because webp and wav
    // share the RIFF container, and it does not know avif or bmp at all.
    val type =
        when {
            bytes.startsWith("RIFF") && bytes.startsWith("WEBP", offset = 8) -> WEBP
            bytes.startsWith("ftyp", offset = 4) && bytes.startsWith("avif", offset = 8) -> AVIF
            bytes.startsWith("BM") -> BMP
            else -> URLConnection.guessContentTypeFromStream(ByteArrayInputStream(bytes))?.let(ContentType::parse)
        }
    return type?.takeIf { it in SUPPORTED_IMAGES }
}

private fun ByteArray.startsWith(
    magic: String,
    offset: Int = 0,
): Boolean =
    size >= offset + magic.length && magic.indices.all { this[offset + it] == magic[it].code.toByte() }
