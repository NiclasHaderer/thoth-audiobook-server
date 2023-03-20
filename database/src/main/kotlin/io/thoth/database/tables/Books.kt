package io.thoth.database.tables

import java.util.*
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date

object TBooks : UUIDTable("Books") {
    val title = varchar("title", 255)
    val releaseDate = date("releaseDate").nullable()
    val publisher = varchar("publisher", 255).nullable()
    val language = varchar("language", 255).nullable()
    val description = text("description").nullable()
    val narrator = varchar("name", 255).nullable()
    val isbn = varchar("isbn", 255).nullable()
    val cover = reference("cover", TImages, onDelete = ReferenceOption.CASCADE).nullable()
    val library = reference("library", TLibraries, onDelete = ReferenceOption.CASCADE)
    val bookMeta = reference("bookMeta", TBookMeta, onDelete = ReferenceOption.CASCADE)
}

class Book(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Book>(TBooks)

    var title by TBooks.title
    var description by TBooks.description
    var releaseDate by TBooks.releaseDate
    var publisher by TBooks.publisher
    var language by TBooks.language
    var narrator by TBooks.narrator
    var isbn by TBooks.isbn
    var coverID by TBooks.cover
    var authors by Author via TAuthorBookMapping
    var series by Series via TSeriesBookMapping
    var genres by Genre via TGenreBookMapping
    val library by Library referencedOn TBooks.library
    val meta by BookMeta referencedOn TBooks.bookMeta
}

object TBookMeta : UUIDTable("BookMeta") {
    val title = varchar("title", 255).nullable()
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val providerRating = float("rating").nullable()
    val releaseDate = date("releaseDate").nullable()
    val publisher = varchar("publisher", 255).nullable()
    val language = varchar("language", 255).nullable()
    val description = text("description").nullable()
    val narrator = varchar("name", 255).nullable()
    val isbn = varchar("isbn", 255).nullable()
    val cover = reference("cover", TImages, onDelete = ReferenceOption.CASCADE).nullable()
}

class BookMeta(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<BookMeta>(TBookMeta)

    var title by TBookMeta.title
    var provider by TBookMeta.provider
    var providerID by TBookMeta.providerID
    var providerRating by TBookMeta.providerRating
    var releaseDate by TBookMeta.releaseDate
    var publisher by TBookMeta.publisher
    var language by TBookMeta.language
    var description by TBookMeta.description
    var narrator by TBookMeta.narrator
    var isbn by TBookMeta.isbn
    var coverID by TBookMeta.cover
}
