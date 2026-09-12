package io.thoth.server.database.tables

import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.server.database.extensions.json
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.date

object BooksTable : LibraryEntityTable("Books", "title") {
    val title get() = name
    val releaseDate = date("releaseDate").nullable()
    val publisher = varchar("publisher", 255).nullable()
    val language = enumerationByName<MetadataLanguage>("language", 255).nullable()
    val description = text("description").nullable()
    val isbn = varchar("isbn", 255).nullable()
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val providerRating = float("rating").nullable()
    val coverID = reference("cover", ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val genres = json<List<String>>("genres").nullable()
    val narrators = json<List<String>>("narrators").nullable()

    val authorsFrom = enumerationByName<MetadataLayer>("authorsFrom", 8).default(MetadataLayer.FILE)
    val seriesFrom = enumerationByName<MetadataLayer>("seriesFrom", 8).default(MetadataLayer.FILE)
}
