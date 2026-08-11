package io.thoth.server.database.views

import io.thoth.models.NamedId
import io.thoth.models.Series
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
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
        id = this[SeriesMetadataView.id],
        library = this[SeriesMetadataView.library],
        title = this[SeriesMetadataView.title],
        totalBooks = this[SeriesMetadataView.totalBooks],
        primaryWorks = this[SeriesMetadataView.primaryWorks],
        description = this[SeriesMetadataView.description],
        provider = this[SeriesMetadataView.provider],
        providerID = this[SeriesMetadataView.providerID],
        coverID = this[SeriesMetadataView.cover],
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
    val genres = seriesGenres(ids)
    return rows.map { row ->
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
            genres = genres[row.id].orEmpty(),
        )
    }
}

context(_: Transaction)
fun seriesAuthors(seriesIds: List<UUID>): Map<UUID, List<NamedId>> =
    BookSeriesView
        .join(BookAuthorView, JoinType.INNER, BookSeriesView.book, BookAuthorView.book)
        .join(AuthorMetadataView, JoinType.INNER, BookAuthorView.author, AuthorMetadataView.id)
        .select(BookSeriesView.series, AuthorMetadataView.id, AuthorMetadataView.name)
        .where { BookSeriesView.series inList seriesIds }
        .groupBy({ it[BookSeriesView.series] }) { NamedId(it[AuthorMetadataView.id], it[AuthorMetadataView.name]) }
        .mapValues { (_, authors) -> authors.distinctBy { it.id } }

/** Likewise its genres, which are those of its books. */
context(_: Transaction)
fun seriesGenres(seriesIds: List<UUID>): Map<UUID, List<String>> =
    BookSeriesView
        .join(BookMetadataView, JoinType.INNER, BookSeriesView.book, BookMetadataView.id)
        .select(BookSeriesView.series, BookMetadataView.genres)
        .where { BookSeriesView.series inList seriesIds }
        .groupBy({ it[BookSeriesView.series] }) { it[BookMetadataView.genres].orEmpty() }
        .mapValues { (_, perBook) -> perBook.flatten().distinctBy { it.lowercase() } }
