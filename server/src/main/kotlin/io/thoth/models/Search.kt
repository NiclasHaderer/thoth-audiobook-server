package io.thoth.models

data class LibrarySearchResult(
    val books: List<BookModel>,
    val series: List<SeriesModel>,
    val authors: List<AuthorModel>,
)
