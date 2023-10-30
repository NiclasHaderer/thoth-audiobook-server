package io.thoth.openapi.common

internal fun getResourceContent(file: String): String {
    return object {}.javaClass.getResourceAsStream(file)?.bufferedReader()?.readText()
        ?: error("Could not load ${file}")
}
