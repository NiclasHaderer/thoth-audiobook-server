package io.thoth.metadata.audible.client

import io.ktor.http.*


fun audibleAsinFromLink(link: String?): String {
    if (link == null) return "invalid"
    return Url(link).encodedPath.split("/").last()
}


fun toImageResAudible(url: String, resolution: Int): String {
    return url.replace(Regex("_SX\\d{2,4}_CR0"), "_SX${resolution}_CR0")
        .replace(Regex(",0,.*"), ",0,0,0__.jpg")
}