package io.thoth.server.database.tables

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date
import java.util.UUID

object TBooks : UUIDTable("Books") {
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
    val coverID = reference("cover", TImages, onDelete = ReferenceOption.CASCADE).nullable()
    val library = reference("library", TLibraries, onDelete = ReferenceOption.CASCADE)
}

class Book(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Book>(TBooks)

    var title by TBooks.title
    var displayTitle by TBooks.displayTitle
    var description by TBooks.description
    var releaseDate by TBooks.releaseDate
    var publisher by TBooks.publisher
    var language by TBooks.language
    var narrator by TBooks.narrator
    var isbn by TBooks.isbn
    var coverID by TBooks.coverID

    // Provider
    var provider by TBooks.provider
    var providerID by TBooks.providerID
    var providerRating by TBooks.providerRating

    // Relations
    var authors by Author via TAuthorBookMapping
    var series by Series via TSeriesBookMapping
    var genres by Genre via TGenreBookMapping
    var library by Library referencedOn TBooks.library
    val tracks by Track referrersOn TTracks.book
}
