package io.thoth.openapi.common

internal fun getResourceContent(path: String): String = getResourceContentOrNull(path) ?: error("Could not load $path")

internal fun getResourceContentOrNull(path: String): String? =
    object {}
        .javaClass
        .getResourceAsStream(path)
        ?.bufferedReader()
        ?.readText()
