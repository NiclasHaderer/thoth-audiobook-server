package io.thoth.openapi.common

internal fun getResourceContent(path: String): String {
    return getResourceContentOrNull(path) ?: error("Could not load $path")
}

internal fun getResourceContentOrNull(path: String): String? {
    return object {}.javaClass.getResourceAsStream(path)?.bufferedReader()?.readText()
}
