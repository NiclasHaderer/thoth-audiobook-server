package io.github.huiibuh.extensions

import io.github.huiibuh.utils.imageFromString
import kotlinx.coroutines.runBlocking

fun String.uriToFile(): ByteArray = runBlocking {
    imageFromString(this@uriToFile)
}
