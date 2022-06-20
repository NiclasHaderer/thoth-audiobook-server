package io.thoth.server.api.search

import io.ktor.resources.*
import kotlinx.serialization.Serializable


@Resource("")
@Serializable()
internal class SearchQuery(
    val q: String?,
    val author: String?,
    val book: String?,
    val series: String?,
)
