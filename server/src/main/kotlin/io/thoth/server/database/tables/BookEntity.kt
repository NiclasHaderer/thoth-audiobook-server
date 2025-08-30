package io.thoth.server.database.tables

import io.thoth.models.Book
import io.thoth.models.NamedId
import io.thoth.models.TitledId
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class BookEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<BookEntity>(BooksTable)

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

    fun toModel(
        authorOrder: SortOrder = SortOrder.ASC,
        seriesOrder: SortOrder = SortOrder.ASC,
    ): Book =
        Book(
            id = id.value,
            title = title,
            description = description,
            providerID = providerID,
            provider = provider,
            providerRating = providerRating,
            coverID = coverID?.value,
            releaseDate = releaseDate,
            narrator = narrator,
            isbn = isbn,
            language = language,
            publisher = publisher,
            authors =
                authors
                    .sortedBy { it.name.lowercase() }
                    .map { NamedId(it.id.value, it.name) }
                    .let { if (authorOrder == SortOrder.DESC) it.reversed() else it },
            series =
                series
                    .sortedBy { it.title.lowercase() }
                    .map { TitledId(it.id.value, it.title) }
                    .let { if (seriesOrder == SortOrder.DESC) it.reversed() else it },
            genres = genres.map { NamedId(it.id.value, it.name) },
            library = NamedId(library.id.value, library.name),
        )
}
