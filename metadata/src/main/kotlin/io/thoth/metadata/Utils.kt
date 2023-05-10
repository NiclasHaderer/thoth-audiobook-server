package io.thoth.metadata

import io.ktor.http.*
import java.util.*

fun String.replaceAll(values: List<Regex>, newValue: String): String {
    var result = this
    values.forEach { result = result.replace(it, newValue) }
    return result
}

fun ParametersBuilder.appendOptional(name: String, value: String?) {
    if (value != null) append(name, value)
}

fun <T> List<T>.saveSubList(startIndex: Int, endIndex: Int? = null): List<T> {
    val searchStartIndex = if (this.size < startIndex) this.size else startIndex
    val searchEndIndex = if (endIndex == null) this.size else if (this.size < endIndex) this.size else endIndex
    return this.subList(searchStartIndex, searchEndIndex)
}

fun <T> T?.optional() = Optional.ofNullable(this)
