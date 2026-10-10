package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object SeriesBookTable : Table("series_book") {
    val series = reference("series_id", SeriesTable, onDelete = ReferenceOption.CASCADE)
    val book = reference("book_id", BookTable, onDelete = ReferenceOption.CASCADE).index()
    val seriesIndex = float("series_index").nullable()
    override val primaryKey = PrimaryKey(series, book)
}
