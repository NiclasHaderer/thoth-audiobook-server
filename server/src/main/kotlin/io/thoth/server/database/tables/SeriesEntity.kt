package io.thoth.server.database.tables

import io.thoth.models.NamedId
import io.thoth.models.Series
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.lowerCase
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

    fun toModel(authorOrder: SortOrder = SortOrder.ASC): Series =
        Series(
            id = id.value,
            title = title,
            description = description,
            providerID = providerID,
            provider = provider,
            coverID = coverID?.value,
            primaryWorks = primaryWorks,
            totalBooks = totalBooks,
            authors =
                authors
                    .orderBy(
                        AuthorTable.name.lowerCase() to authorOrder,
                    ).map { NamedId(it.id.value, it.name) },
            genres = genres.map { NamedId(it.id.value, it.name) },
            library = NamedId(library.id.value, library.name),
        )
}
