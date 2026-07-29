package io.thoth.server.common.extensions

import me.xdrop.fuzzywuzzy.FuzzySearch

private const val MATCH_THRESHOLD = 80

fun <T> Iterable<T>.fuzzy(
    query: String,
    getValues: (T) -> List<String>,
): List<T> =
    this
        .map { item -> item to getValues(item).maxOfOrNull { FuzzySearch.weightedRatio(query, it) } }
        .filter { (_, match) -> match != null && match > MATCH_THRESHOLD }
        .sortedByDescending { (_, match) -> match }
        .map { (item, _) -> item }
