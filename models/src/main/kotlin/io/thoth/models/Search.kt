package io.thoth.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchModel(
    val books: List<BookModel>,
    val series: List<SeriesModel>,
    val authors: List<AuthorModel>,
)
