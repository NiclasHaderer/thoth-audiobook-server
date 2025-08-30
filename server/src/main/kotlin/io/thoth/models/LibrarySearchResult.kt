package io.thoth.models

data class LibrarySearchResult(
    val books: List<Book>,
    val series: List<Series>,
    val authors: List<Author>,
)
