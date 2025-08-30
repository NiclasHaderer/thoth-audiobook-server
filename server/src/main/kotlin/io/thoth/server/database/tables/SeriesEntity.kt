package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

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
    var books by BookEntity via SeriesBookTable
    var genres by GenreEntity via GenreSeriesTable
    var library by LibraryEntity referencedOn SeriesTable.library
}
