package io.thoth.server.database.tables

import io.thoth.models.NamedId
import io.thoth.models.Series
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
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

    /** [title] stays what the files were matched on; [displayTitle] is what a metadata match or an edit sets. */
    val displayedTitle: String
        get() = displayTitle ?: title

    fun toModel(authorOrder: SortOrder = SortOrder.ASC): Series =
        Series(
            id = id.value,
            title = displayedTitle,
            description = description,
            providerID = providerID,
            provider = provider,
            coverID = coverID?.value,
            primaryWorks = primaryWorks,
            totalBooks = totalBooks,
            authors =
                authors
                    .orderBy(
                        AuthorTable.displayedName.lowerCase() to authorOrder,
                    ).map { NamedId(it.id.value, it.displayedName) },
            genres = genres.map { NamedId(it.id.value, it.name) },
        )
}
