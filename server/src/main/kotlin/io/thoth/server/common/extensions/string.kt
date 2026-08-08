package io.thoth.server.common.extensions

import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
fun String.replaceAll(
    values: List<Regex>,
    newValue: String,
): String {
    var result = this
    values.forEach { result = result.replace(it, newValue) }
    return result
}

private val DIGIT_RUN = "\\d+".toRegex()

/** Orders strings the way a file browser does, so "Chapter 2" comes before "Chapter 10" instead of after it. */
val naturalOrder: Comparator<String> =
    Comparator { left, right ->
        val leftParts = left.splitOnDigits()
        val rightParts = right.splitOnDigits()
        leftParts
            .zip(rightParts) { a, b ->
                val numbers = a.toBigIntegerOrNull()?.let { x -> b.toBigIntegerOrNull()?.let { y -> x to y } }
                numbers?.first?.compareTo(numbers.second) ?: String.CASE_INSENSITIVE_ORDER.compare(a, b)
            }.firstOrNull { it != 0 } ?: leftParts.size.compareTo(rightParts.size)
    }

private fun String.splitOnDigits(): List<String> {
    val parts = mutableListOf<String>()
    var index = 0
    for (match in DIGIT_RUN.findAll(this)) {
        if (match.range.first > index) parts += substring(index, match.range.first)
        parts += match.value
        index = match.range.last + 1
    }
    if (index < length) parts += substring(index)
    return parts
}

fun String.toCron(): Cron {
    val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
    val parser = CronParser(cronDefinition)
    return parser.parse(this)
}
