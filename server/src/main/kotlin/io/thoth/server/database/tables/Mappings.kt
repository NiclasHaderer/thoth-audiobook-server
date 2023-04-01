package io.thoth.server.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object TAuthorBookMapping : Table("AuthorBookMapping") {
    val author = reference("author", TAuthors, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", TBooks, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(author, book)
}

object TGenreBookMapping : Table("GenreBookMapping") {
    val genre = reference("genre", TGenres, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", TBooks, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(genre, book)
}

object TGenreSeriesMapping : Table("GenreSeriesMapping") {
    val genre = reference("genre", TGenres, onDelete = ReferenceOption.CASCADE)
    val series = reference("Series", TSeries, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(genre, series)
}

object TSeriesBookMapping : Table("SeriesBookMapping") {
    val series = reference("series", TSeries, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", TBooks, onDelete = ReferenceOption.CASCADE)
    val seriesIndex = float("index").nullable()
    override val primaryKey = PrimaryKey(series, book)
}

object TSeriesAuthorMapping : Table("SeriesAuthorMapping") {
    val series = reference("series", TSeries, onDelete = ReferenceOption.CASCADE)
    val author = reference("author", TAuthors, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(series, author)
}
