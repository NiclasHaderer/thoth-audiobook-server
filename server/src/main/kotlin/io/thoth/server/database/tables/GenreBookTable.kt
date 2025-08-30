package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable

object GenreBookTable : CompositeIdTable("GenreBook") {
    val genre = reference("genre", GenresTable, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", BooksTable, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(genre, book)

    init {
        addIdColumn(book)
        addIdColumn(genre)
    }
}
