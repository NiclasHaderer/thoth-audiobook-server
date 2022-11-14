package io.thoth.database.tables.meta

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table


object TMetaAuthorBookMapping : Table("MetaAuthorBookMapping") {
    val author = reference("author", TMetaAuthors, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", TMetaBooks, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(
        author, book
    )
}

object TMetaGenreAuthorMapping : Table("MetaGenreAuthorMapping") {
    val genre = reference("genre", TMetaGenres, onDelete = ReferenceOption.CASCADE)
    val author = reference("author", TMetaAuthors, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(
        genre, author
    )
}

object TMetaGenreBookMapping : Table("MetaGenreBookMapping") {
    val genre = reference("genre", TMetaGenres, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", TMetaBooks, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(
        genre, book
    )
}


object TMetaGenreSeriesMapping : Table("MetaGenreSeriesMapping") {
    val genre = reference("genre", TMetaGenres, onDelete = ReferenceOption.CASCADE)
    val series = reference("Series", TMetaSeries, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(
        genre, series
    )
}


object TMetaSeriesBookMapping : Table("MetaSeriesBookMapping") {
    val series = reference("series", TMetaSeries, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", TMetaBooks, onDelete = ReferenceOption.CASCADE)
    val seriesIndex = float("index").nullable()
    override val primaryKey = PrimaryKey(
        series, book
    )
}

object TMetaSeriesAuthorMapping : Table("MetaSeriesAuthorMapping") {
    val series = reference("series", TMetaSeries, onDelete = ReferenceOption.CASCADE)
    val author = reference("author", TMetaAuthors, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(
        series, author
    )
}
