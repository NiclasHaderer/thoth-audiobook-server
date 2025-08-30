package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable

object SeriesAuthorTable : CompositeIdTable("SeriesAuthor") {
    val series = reference("series", SeriesTable, onDelete = ReferenceOption.CASCADE)
    val author = reference("author", AuthorTable, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(series, author)

    init {
        addIdColumn(author)
        addIdColumn(series)
    }
}
