package io.thoth.models

data class SearchModel(
    val books: List<BookModel>,
    val series: List<SeriesModel>,
    val authors: List<AuthorModel>,
)
