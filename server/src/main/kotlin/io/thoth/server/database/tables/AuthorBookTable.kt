package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object AuthorBookTable : Table("author_book") {
    val author = reference("author_id", AuthorTable, onDelete = ReferenceOption.CASCADE)
    val book = reference("book_id", BookTable, onDelete = ReferenceOption.CASCADE).index()

    val addedBy = enumerationByName<MetadataLayer>("added_by", 8)
    override val primaryKey = PrimaryKey(author, book, addedBy)
}
