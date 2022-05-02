package io.thoth.common.extensions

import io.thoth.common.utils.imageFromString


suspend fun String.uriToFile(): ByteArray = imageFromString(this)
