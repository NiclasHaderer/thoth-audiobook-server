package io.github.huiibuh.api.search

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam

internal data class SearchQuery(
    @QueryParam("A general query. Looks in everything") val q: String?,
    @QueryParam("Search for an author. Can be combined with the other parameters except q.") val author: String?,
    @QueryParam("Search for a book. Can be combined with the other parameters except q.") val book: String?,
    @QueryParam("Search for a series. Can be combined with the other parameters except q.") val series: String?,
)
