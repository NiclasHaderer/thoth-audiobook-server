package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.date
import java.util.UUID

object BooksTable : UUIDTable("Books") {
    val title = varchar("title", 255)
    val displayTitle = varchar("displayTitle", 255).nullable()
    val releaseDate = date("releaseDate").nullable()
    val publisher = varchar("publisher", 255).nullable()
    val language = varchar("language", 255).nullable()
    val description = text("description").nullable()
    val narrator = varchar("name", 255).nullable()
    val isbn = varchar("isbn", 255).nullable()

    // Provider
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val providerRating = float("rating").nullable()

    // Relations
    val coverID = reference("cover", ImageTable, onDelete = ReferenceOption.CASCADE).nullable()
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE)
}

class BookeEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<BookeEntity>(BooksTable)

    var title by BooksTable.title
    var displayTitle by BooksTable.displayTitle
    var description by BooksTable.description
    var releaseDate by BooksTable.releaseDate
    var publisher by BooksTable.publisher
    var language by BooksTable.language
    var narrator by BooksTable.narrator
    var isbn by BooksTable.isbn
    var coverID by BooksTable.coverID

    // Provider
    var provider by BooksTable.provider
    var providerID by BooksTable.providerID
    var providerRating by BooksTable.providerRating

    // Relations
    var authors by AuthorEntity via AuthorBookTable
    var series by SeriesEntity via SeriesBookTable
    var genres by GenreEntity via GenreBookTable
    var library by LibraryEntity referencedOn BooksTable.library
    val tracks by TrackEntity referrersOn TracksTable.book
}
