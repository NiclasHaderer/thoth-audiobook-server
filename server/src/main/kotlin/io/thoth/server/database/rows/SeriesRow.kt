package io.thoth.server.database.rows

import io.thoth.models.NamedId
import io.thoth.models.Series
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.resolvedSeriesLinks
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import java.util.UUID

data class SeriesRow(
    val id: UUID,
    val library: UUID,
    val title: String,
    val totalBooks: Int?,
    val primaryWorks: Int?,
    val description: String?,
    val provider: String?,
    val providerID: String?,
    val coverID: UUID?,
)

fun ResultRow.toSeriesRow(): SeriesRow =
    SeriesRow(
        id = this[SeriesTable.id].value,
        library = this[SeriesTable.library].value,
        title = this[SeriesTable.title],
        totalBooks = this[SeriesTable.totalBooks],
        primaryWorks = this[SeriesTable.primaryWorks],
        description = this[SeriesTable.description],
        provider = this[SeriesTable.provider],
        providerID = this[SeriesTable.providerID],
        coverID = this[SeriesTable.coverID]?.value,
    )

context(_: Transaction)
fun SeriesRow.toModel(authorOrder: SortOrder = SortOrder.ASC): Series =
    seriesToModels(listOf(this), authorOrder).single()

context(_: Transaction)
fun seriesToModels(
    rows: List<SeriesRow>,
    authorOrder: SortOrder = SortOrder.ASC,
): List<Series> {
    if (rows.isEmpty()) return emptyList()
    val ids = rows.map { it.id }
    val authors = seriesAuthors(ids)
    val books = seriesBooks(ids)
    return rows.map { row ->
        val ownBooks = books[row.id].orEmpty()
        Series(
            id = row.id,
            libraryId = row.library,
            title = row.title,
            description = row.description,
            providerID = row.providerID,
            provider = row.provider,
            coverID = row.coverID,
            primaryWorks = row.primaryWorks,
            totalBooks = row.totalBooks,
            authors =
                (authors[row.id] ?: emptyList())
                    .sortedBy { it.name.lowercase() }
                    .let { if (authorOrder == SortOrder.DESC) it.reversed() else it },
            genres = ownBooks.flatMap { it.genres }.distinctBy { it.lowercase() },
            bookCoverIDs = ownBooks.mapNotNull { it.coverID },
        )
    }
}

context(_: Transaction)
fun seriesAuthors(seriesIds: List<UUID>): Map<UUID, List<NamedId>> =
    resolvedSeriesLinks
        .join(AuthorBookTable, JoinType.INNER, SeriesBookTable.book, AuthorBookTable.book) {
            AuthorBookTable.addedBy eq BooksTable.authorsFrom
        }.join(AuthorTable, JoinType.INNER, AuthorBookTable.authors, AuthorTable.id)
        .select(SeriesBookTable.series, AuthorTable.id, AuthorTable.name)
        .where { SeriesBookTable.series inList seriesIds }
        .groupBy({ it[SeriesBookTable.series].value }) {
            NamedId(it[AuthorTable.id].value, it[AuthorTable.name])
        }.mapValues { (_, authors) -> authors.distinctBy { it.id } }

data class SeriesBook(
    val coverID: UUID?,
    val genres: List<String>,
)

context(_: Transaction)
fun seriesBooks(seriesIds: List<UUID>): Map<UUID, List<SeriesBook>> =
    resolvedSeriesLinks
        .select(SeriesBookTable.series, SeriesBookTable.seriesIndex, BooksTable.coverID, BooksTable.genres)
        .where { (SeriesBookTable.series inList seriesIds) and BooksTable.visible }
        .orderBy(SeriesBookTable.seriesIndex to SortOrder.ASC_NULLS_LAST)
        .groupBy({ it[SeriesBookTable.series].value }) {
            SeriesBook(it[BooksTable.coverID]?.value, it[BooksTable.genres].orEmpty())
        }
