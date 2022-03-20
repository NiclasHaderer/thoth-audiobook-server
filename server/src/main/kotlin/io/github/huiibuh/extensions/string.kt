package io.github.huiibuh.extensions

import io.github.huiibuh.utils.imageFromString

suspend fun String.uriToFile(): ByteArray = imageFromString(this)
