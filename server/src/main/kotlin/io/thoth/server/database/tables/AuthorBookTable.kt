package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object AuthorBookTable : Table("AuthorBook") {
    val authors = reference("author", AuthorTable, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", BooksTable, onDelete = ReferenceOption.CASCADE).index()

    val addedBy = enumerationByName<MetadataLayer>("addedBy", 8)
    override val primaryKey = PrimaryKey(authors, book, addedBy)
}
