package io.thoth.server.api.search

import io.ktor.resources.*


@Resource("")
internal class SearchQuery(
    val q: String? = null,
    val author: String? = null,
    val book: String? = null,
    val series: String? = null,
)
