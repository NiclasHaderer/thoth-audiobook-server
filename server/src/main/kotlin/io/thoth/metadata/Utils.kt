package io.thoth.metadata

import io.ktor.http.ParametersBuilder

internal fun ParametersBuilder.appendOptional(
    name: String,
    value: String?,
) {
    if (value != null) append(name, value)
}
