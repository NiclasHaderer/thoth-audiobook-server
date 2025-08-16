package io.thoth.server.common.extensions

import me.xdrop.fuzzywuzzy.FuzzySearch
import org.jetbrains.exposed.sql.SizedIterable

class FuzzyResult<T>(
    val match: Int,
    val value: T,
) {
    val matches: Boolean
        get() = match > 80
}

fun <T> String.fuzzy(
    query: String,
    value: T,
): FuzzyResult<T> {
    val fuz = FuzzySearch.partialRatio(query.lowercase(), this.lowercase())
    return FuzzyResult(fuz, value)
}

fun <T> SizedIterable<T>.fuzzy(
    query: String,
    getValues: (T) -> List<String>,
): List<T> =
    this
        .mapNotNull { item -> getValues(item).map { it.fuzzy(query, item) }.maxByOrNull { it.match } }
        .filter { it.matches }
        .sortedByDescending { it.match }
        .map { it.value }
