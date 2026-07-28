package io.thoth.metadata

import io.ktor.http.ParametersBuilder

internal fun String.replaceAll(
    values: List<Regex>,
    newValue: String,
): String {
    var result = this
    values.forEach { result = result.replace(it, newValue) }
    return result
}

internal fun ParametersBuilder.appendOptional(
    name: String,
    value: String?,
) {
    if (value != null) append(name, value)
}
