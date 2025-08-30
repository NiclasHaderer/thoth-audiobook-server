package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.date

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
