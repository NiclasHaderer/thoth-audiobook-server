package io.thoth.server.database.tables

import io.thoth.models.LibraryPermissions
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass

object AuthorBookTable : CompositeIdTable("AuthorBook") {
    val authors = reference("author", AuthorTable, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", BooksTable, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(authors, book)

    init {
        addIdColumn(book)
        addIdColumn(authors)
    }
}

object GenreBookTable : CompositeIdTable("GenreBook") {
    val genre = reference("genre", GenresTable, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", BooksTable, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(genre, book)

    init {
        addIdColumn(book)
        addIdColumn(genre)
    }
}

object GenreSeriesTable : CompositeIdTable("GenreSeries") {
    val genre = reference("genre", GenresTable, onDelete = ReferenceOption.CASCADE)
    val series = reference("Series", SeriesTable, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(genre, series)

    init {
        addIdColumn(series)
        addIdColumn(genre)
    }
}

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

object SeriesAuthorTable : CompositeIdTable("SeriesAuthor") {
    val series = reference("series", SeriesTable, onDelete = ReferenceOption.CASCADE)
    val author = reference("author", AuthorTable, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(series, author)

    init {
        addIdColumn(author)
        addIdColumn(series)
    }
}

object LibraryUserTable : CompositeIdTable("LibraryUser") {
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE)
    val user = reference("user", UsersTable, onDelete = ReferenceOption.CASCADE)
    var permissions = enumeration<LibraryPermissions>("permissions")
    override val primaryKey = PrimaryKey(library, user)

    init {
        addIdColumn(user)
        addIdColumn(library)
    }
}

class LibraryUserEntity(
    id: EntityID<CompositeID>,
) : CompositeEntity(id) {
    companion object : CompositeEntityClass<LibraryUserEntity>(LibraryUserTable)

    var permissions by LibraryUserTable.permissions
    var library by LibraryEntity referencedOn LibraryUserTable.library
    var user by UserEntity referencedOn LibraryUserTable.user
}
