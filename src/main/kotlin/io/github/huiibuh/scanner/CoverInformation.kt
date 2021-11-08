package io.github.huiibuh.scanner

data class CoverInformation(
    val cover: ByteArray,
    val mimetype: String,
) {
    val extension: String
        get() = mimetype.split("/").last()
}


