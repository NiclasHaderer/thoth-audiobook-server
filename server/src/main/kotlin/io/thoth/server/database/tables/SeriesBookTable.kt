package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object SeriesBookTable : Table("SeriesBook") {
    val series = reference("series", SeriesTable, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", BooksTable, onDelete = ReferenceOption.CASCADE).index()
    val seriesIndex = float("index").nullable()
    val addedBy = enumerationByName<MetadataLayer>("addedBy", 8)
    override val primaryKey = PrimaryKey(series, book, addedBy)
}
