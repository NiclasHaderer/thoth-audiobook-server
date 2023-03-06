package io.thoth.server.api.search

import io.ktor.resources.*

@Resource("")
data class SearchQuery(
    val q: String? = null,
    val author: String? = null,
    val book: String? = null,
    val series: String? = null,
)
