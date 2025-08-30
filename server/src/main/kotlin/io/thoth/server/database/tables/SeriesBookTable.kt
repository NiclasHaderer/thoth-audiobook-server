package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable

object SeriesBookTable : CompositeIdTable("SeriesBook") {
    val series = reference("series", SeriesTable, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", BooksTable, onDelete = ReferenceOption.CASCADE)
    val seriesIndex = float("index").nullable()
    override val primaryKey = PrimaryKey(series, book)

    init {
        addIdColumn(book)
        addIdColumn(series)
    }
}
