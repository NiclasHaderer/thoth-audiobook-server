package io.thoth.server.database.rows

import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.models.Book
import io.thoth.models.NamedId
import io.thoth.models.PlayStatus
import io.thoth.models.TitledId
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TrackTable
import io.thoth.server.database.tables.UserBookProgressRow
import io.thoth.server.database.tables.UserBookProgressTable
import io.thoth.server.database.tables.resolvedAuthorLinks
import io.thoth.server.database.tables.resolvedSeriesLinks
import io.thoth.server.database.tables.toUserBookProgressRow
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDate
import java.util.UUID

data class BookRow(
    val id: UUID,
    val library: UUID,
    val title: String,
    val releaseDate: LocalDate?,
    val publisher: String?,
    val language: MetadataLanguage?,
    val description: String?,
    val narrators: List<String>,
    val isbn: String?,
    val provider: String?,
    val providerID: String?,
    val providerRating: Float?,
    val coverID: UUID?,
    val genres: List<String>,
)

fun ResultRow.toBookRow(): BookRow =
    BookRow(
        id = this[BookTable.id].value,
        library = this[BookTable.library].value,
        title = this[BookTable.title],
        releaseDate = this[BookTable.releaseDate],
        publisher = this[BookTable.publisher],
        language = this[BookTable.language],
        description = this[BookTable.description],
        narrators = this[BookTable.narrators].orEmpty(),
        isbn = this[BookTable.isbn],
        provider = this[BookTable.provider],
        providerID = this[BookTable.providerId],
        providerRating = this[BookTable.providerRating],
        coverID = this[BookTable.coverId]?.value,
        genres = this[BookTable.genres].orEmpty(),
    )

context(_: Transaction)
fun BookRow.toModel(
    userId: UUID,
    authorOrder: SortOrder = SortOrder.ASC,
    seriesOrder: SortOrder = SortOrder.ASC,
): Book = booksToModels(listOf(this), userId, authorOrder, seriesOrder).single()

context(_: Transaction)
fun booksToModels(
    rows: List<BookRow>,
    userId: UUID,
    authorOrder: SortOrder = SortOrder.ASC,
    seriesOrder: SortOrder = SortOrder.ASC,
): List<Book> {
    if (rows.isEmpty()) return emptyList()
    val ids = rows.map { it.id }
    val authors = bookAuthors(ids)
    val series = bookSeries(ids)
    val durations = bookDurations(ids)
    val progress = bookProgress(userId, ids)
    return rows.map { row ->
        val bookProgress = progress[row.id]
        Book(
            id = row.id,
            libraryId = row.library,
            title = row.title,
            description = row.description,
            providerID = row.providerID,
            provider = row.provider,
            providerRating = row.providerRating,
            coverID = row.coverID,
            releaseDate = row.releaseDate,
            narrators = row.narrators,
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
            durationMs = durations[row.id] ?: 0,
            positionMs = bookProgress?.positionMs ?: 0,
            status = bookProgress?.status ?: PlayStatus.UNPLAYED,
        )
    }
}

context(_: Transaction)
fun bookDurations(bookIds: List<UUID>): Map<UUID, Long> {
    val total = TrackTable.durationMs.sum()
    return TrackTable
        .select(TrackTable.book, total)
        .where { TrackTable.book inList bookIds }
        .groupBy(TrackTable.book)
        .associate { it[TrackTable.book].value to (it[total] ?: 0L) }
}

context(_: Transaction)
fun bookProgress(
    userId: UUID,
    bookIds: List<UUID>,
): Map<UUID, UserBookProgressRow> =
    UserBookProgressTable
        .selectAll()
        .where { (UserBookProgressTable.user eq userId) and (UserBookProgressTable.book inList bookIds) }
        .associate { it[UserBookProgressTable.book].value to it.toUserBookProgressRow() }

context(_: Transaction)
fun bookAuthors(bookIds: List<UUID>): Map<UUID, List<NamedId>> =
    resolvedAuthorLinks
        .join(AuthorTable, JoinType.INNER, AuthorBookTable.author, AuthorTable.id)
        .select(AuthorBookTable.book, AuthorTable.id, AuthorTable.name)
        .where { AuthorBookTable.book inList bookIds }
        .groupBy({ it[AuthorBookTable.book].value }) {
            NamedId(it[AuthorTable.id].value, it[AuthorTable.name])
        }

context(_: Transaction)
fun bookSeries(bookIds: List<UUID>): Map<UUID, List<TitledId>> =
    resolvedSeriesLinks
        .join(SeriesTable, JoinType.INNER, SeriesBookTable.series, SeriesTable.id)
        .select(SeriesBookTable.book, SeriesTable.id, SeriesTable.title)
        .where { SeriesBookTable.book inList bookIds }
        .groupBy({ it[SeriesBookTable.book].value }) {
            TitledId(it[SeriesTable.id].value, it[SeriesTable.title])
        }
