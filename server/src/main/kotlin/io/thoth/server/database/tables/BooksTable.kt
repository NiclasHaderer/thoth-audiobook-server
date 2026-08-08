package io.thoth.server.database.tables

import io.thoth.models.Book
import io.thoth.models.NamedId
import io.thoth.models.TitledId
import org.jetbrains.exposed.v1.core.Coalesce
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDate
import java.util.UUID

object BooksTable : UUIDTable("Books") {
    val title = text("title")
    val displayTitle = varchar("displayTitle", 255).nullable()
    val releaseDate = date("releaseDate").nullable()
    val publisher = varchar("publisher", 255).nullable()
    val language = varchar("language", 255).nullable()
    val description = text("description").nullable()
    val narrator = varchar("narrator", 255).nullable()
    val isbn = varchar("isbn", 255).nullable()

    // Provider
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val providerRating = float("rating").nullable()

    // Relations
    val coverID = reference("cover", ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE).index()

    val displayedTitle = Coalesce(displayTitle, title)
}

data class BookRow(
    val id: UUID,
    val title: String,
    val displayTitle: String?,
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
    val library: UUID,
) {
    val displayedTitle: String
        get() = displayTitle ?: title
}

fun ResultRow.toBookRow(): BookRow =
    BookRow(
        id = this[BooksTable.id].value,
        title = this[BooksTable.title],
        displayTitle = this[BooksTable.displayTitle],
        releaseDate = this[BooksTable.releaseDate],
        publisher = this[BooksTable.publisher],
        language = this[BooksTable.language],
        description = this[BooksTable.description],
        narrator = this[BooksTable.narrator],
        isbn = this[BooksTable.isbn],
        provider = this[BooksTable.provider],
        providerID = this[BooksTable.providerID],
        providerRating = this[BooksTable.providerRating],
        coverID = this[BooksTable.coverID]?.value,
        library = this[BooksTable.library].value,
    )

context(_: Transaction)
fun BooksTable.insert(row: BookRow): UUID {
    insert { write(it, row) }
    return row.id
}

context(_: Transaction)
fun BooksTable.update(row: BookRow) {
    update({ BooksTable.id eq row.id }) { write(it, row) }
}

private fun write(
    stmt: UpdateBuilder<*>,
    row: BookRow,
) {
    stmt[BooksTable.id] = row.id
    stmt[BooksTable.title] = row.title
    stmt[BooksTable.displayTitle] = row.displayTitle
    stmt[BooksTable.releaseDate] = row.releaseDate
    stmt[BooksTable.publisher] = row.publisher
    stmt[BooksTable.language] = row.language
    stmt[BooksTable.description] = row.description
    stmt[BooksTable.narrator] = row.narrator
    stmt[BooksTable.isbn] = row.isbn
    stmt[BooksTable.provider] = row.provider
    stmt[BooksTable.providerID] = row.providerID
    stmt[BooksTable.providerRating] = row.providerRating
    stmt[BooksTable.coverID] = row.coverID
    stmt[BooksTable.library] = row.library
}

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
    val genres = bookGenres(ids)
    return rows.map { row ->
        Book(
            id = row.id,
            title = row.displayedTitle,
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
            genres = genres[row.id] ?: emptyList(),
        )
    }
}

context(_: Transaction)
fun bookAuthors(bookIds: List<UUID>): Map<UUID, List<NamedId>> =
    (AuthorBookTable innerJoin AuthorTable)
        .select(AuthorBookTable.book, AuthorTable.id, AuthorTable.name, AuthorTable.displayName)
        .where { AuthorBookTable.book inList bookIds }
        .groupBy({ it[AuthorBookTable.book].value }) {
            NamedId(it[AuthorTable.id].value, it[AuthorTable.displayName] ?: it[AuthorTable.name])
        }

context(_: Transaction)
fun bookSeries(bookIds: List<UUID>): Map<UUID, List<TitledId>> =
    (SeriesBookTable innerJoin SeriesTable)
        .select(SeriesBookTable.book, SeriesTable.id, SeriesTable.title, SeriesTable.displayTitle)
        .where { SeriesBookTable.book inList bookIds }
        .groupBy({ it[SeriesBookTable.book].value }) {
            TitledId(it[SeriesTable.id].value, it[SeriesTable.displayTitle] ?: it[SeriesTable.title])
        }

context(_: Transaction)
private fun bookGenres(bookIds: List<UUID>): Map<UUID, List<NamedId>> =
    (GenreBookTable innerJoin GenresTable)
        .select(GenreBookTable.book, GenresTable.id, GenresTable.name)
        .where { GenreBookTable.book inList bookIds }
        .groupBy({ it[GenreBookTable.book].value }) {
            NamedId(it[GenresTable.id].value, it[GenresTable.name])
        }
