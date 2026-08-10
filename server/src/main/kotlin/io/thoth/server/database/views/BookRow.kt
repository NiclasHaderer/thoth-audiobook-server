package io.thoth.server.database.views

import io.thoth.models.Book
import io.thoth.models.NamedId
import io.thoth.models.TitledId
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import java.time.LocalDate
import java.util.UUID

data class BookRow(
    val id: UUID,
    val library: UUID,
    val title: String,
    val releaseDate: LocalDate?,
    val publisher: String?,
    val language: String?,
    val description: String?,
    val narrator: String?,
    val isbn: String?,
    val provider: String?,
    val providerID: String?,
    val providerRating: Float?,
    val coverID: UUID?,
    val genres: List<String>,
)

fun ResultRow.toBookRow(): BookRow =
    BookRow(
        id = this[BookMetadataView.id],
        library = this[BookMetadataView.library],
        title = this[BookMetadataView.title],
        releaseDate = this[BookMetadataView.releaseDate],
        publisher = this[BookMetadataView.publisher],
        language = this[BookMetadataView.language],
        description = this[BookMetadataView.description],
        narrator = this[BookMetadataView.narrator],
        isbn = this[BookMetadataView.isbn],
        provider = this[BookMetadataView.provider],
        providerID = this[BookMetadataView.providerID],
        providerRating = this[BookMetadataView.providerRating],
        coverID = this[BookMetadataView.cover],
        genres = this[BookMetadataView.genres].orEmpty(),
    )

context(_: Transaction)
fun BookRow.toModel(
    authorOrder: SortOrder = SortOrder.ASC,
    seriesOrder: SortOrder = SortOrder.ASC,
): Book = booksToModels(listOf(this), authorOrder, seriesOrder).single()

context(_: Transaction)
fun booksToModels(
    rows: List<BookRow>,
    authorOrder: SortOrder = SortOrder.ASC,
    seriesOrder: SortOrder = SortOrder.ASC,
): List<Book> {
    if (rows.isEmpty()) return emptyList()
    val ids = rows.map { it.id }
    val authors = bookAuthors(ids)
    val series = bookSeries(ids)
    return rows.map { row ->
        Book(
            id = row.id,
            title = row.title,
            description = row.description,
            providerID = row.providerID,
            provider = row.provider,
            providerRating = row.providerRating,
            coverID = row.coverID,
            releaseDate = row.releaseDate,
            narrator = row.narrator,
            isbn = row.isbn,
            language = row.language,
            publisher = row.publisher,
            authors =
                (authors[row.id] ?: emptyList())
                    .sortedBy { it.name.lowercase() }
                    .let { if (authorOrder == SortOrder.DESC) it.reversed() else it },
            series =
                (series[row.id] ?: emptyList())
                    .sortedBy { it.title.lowercase() }
                    .let { if (seriesOrder == SortOrder.DESC) it.reversed() else it },
            genres = row.genres,
        )
    }
}

context(_: Transaction)
fun bookAuthors(bookIds: List<UUID>): Map<UUID, List<NamedId>> =
    BookAuthorView
        .join(AuthorMetadataView, JoinType.INNER, BookAuthorView.author, AuthorMetadataView.id)
        .select(BookAuthorView.book, AuthorMetadataView.id, AuthorMetadataView.name)
        .where { BookAuthorView.book inList bookIds }
        .groupBy({ it[BookAuthorView.book] }) { NamedId(it[AuthorMetadataView.id], it[AuthorMetadataView.name]) }

context(_: Transaction)
fun bookSeries(bookIds: List<UUID>): Map<UUID, List<TitledId>> =
    BookSeriesView
        .join(SeriesMetadataView, JoinType.INNER, BookSeriesView.series, SeriesMetadataView.id)
        .select(BookSeriesView.book, SeriesMetadataView.id, SeriesMetadataView.title)
        .where { BookSeriesView.book inList bookIds }
        .groupBy({ it[BookSeriesView.book] }) { TitledId(it[SeriesMetadataView.id], it[SeriesMetadataView.title]) }
