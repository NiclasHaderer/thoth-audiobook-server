package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable

object AuthorBookTable : CompositeIdTable("AuthorBook") {
    val authors = reference("author", AuthorTable, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", BooksTable, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(authors, book)

    init {
        addIdColumn(book)
        addIdColumn(authors)
    }
}
