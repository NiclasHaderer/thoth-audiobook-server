package io.thoth.models

data class Genre(
    val name: String,
    val bookCount: Int,
)

data class GenreDetailed(
    val name: String,
    val books: List<Book>,
)
