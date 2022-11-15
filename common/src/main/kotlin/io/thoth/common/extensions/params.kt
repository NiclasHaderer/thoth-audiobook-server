package io.thoth.common.extensions

import io.ktor.http.*

fun ParametersBuilder.appendOptional(name: String, value: String?) {
    if (value != null) append(name, value)
}