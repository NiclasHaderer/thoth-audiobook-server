package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

object SeriesTable : UUIDTable("Series") {
    val title = varchar("title", 255)
    val displayTitle = varchar("displayTitle", 255).nullable()
    val totalBooks = integer("totalBooks").nullable()
    val primaryWorks = integer("primaryWorks").nullable()
    val description = text("description").nullable()

    // Provider
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()

    // Relations
    val coverID = reference("cover", ImageTable).nullable()
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE)
}

class SeriesEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SeriesEntity>(SeriesTable)

    var title by SeriesTable.title
    var displayTitle by SeriesTable.displayTitle
    var totalBooks by SeriesTable.totalBooks
    var primaryWorks by SeriesTable.primaryWorks
    var coverID by SeriesTable.coverID
    var description by SeriesTable.description

    // Provider
    var provider by SeriesTable.provider
    var providerID by SeriesTable.providerID

    // Relations
    var authors by AuthorEntity via SeriesAuthorTable
    var books by BookeEntity via SeriesBookTable
    var genres by GenreEntity via GenreSeriesTable
    var library by LibraryEntity referencedOn SeriesTable.library
}
