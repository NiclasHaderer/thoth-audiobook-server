package io.thoth.server.database.tables

import io.thoth.models.NamedId
import io.thoth.models.Series
import org.jetbrains.exposed.v1.core.Coalesce
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
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
    val coverID = reference("cover", ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE).index()

    val displayedTitle = Coalesce(displayTitle, title)
}

data class SeriesRow(
    val id: UUID,
    val title: String,
    val displayTitle: String?,
    val totalBooks: Int?,
    val primaryWorks: Int?,
    val description: String?,
    val provider: String?,
    val providerID: String?,
    val coverID: UUID?,
    val library: UUID,
) {
    val displayedTitle: String
        get() = displayTitle ?: title
}

fun ResultRow.toSeriesRow(): SeriesRow =
    SeriesRow(
        id = this[SeriesTable.id].value,
        title = this[SeriesTable.title],
        displayTitle = this[SeriesTable.displayTitle],
        totalBooks = this[SeriesTable.totalBooks],
        primaryWorks = this[SeriesTable.primaryWorks],
        description = this[SeriesTable.description],
        provider = this[SeriesTable.provider],
        providerID = this[SeriesTable.providerID],
        coverID = this[SeriesTable.coverID]?.value,
        library = this[SeriesTable.library].value,
    )

context(_: Transaction)
fun SeriesTable.insert(row: SeriesRow): UUID {
    insert { write(it, row) }
    return row.id
}

context(_: Transaction)
fun SeriesTable.update(row: SeriesRow) {
    update({ SeriesTable.id eq row.id }) { write(it, row) }
}

private fun write(
    stmt: UpdateBuilder<*>,
    row: SeriesRow,
) {
    stmt[SeriesTable.id] = row.id
    stmt[SeriesTable.title] = row.title
    stmt[SeriesTable.displayTitle] = row.displayTitle
    stmt[SeriesTable.totalBooks] = row.totalBooks
    stmt[SeriesTable.primaryWorks] = row.primaryWorks
    stmt[SeriesTable.description] = row.description
    stmt[SeriesTable.provider] = row.provider
    stmt[SeriesTable.providerID] = row.providerID
    stmt[SeriesTable.coverID] = row.coverID
    stmt[SeriesTable.library] = row.library
}

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
    val authors =
        (SeriesAuthorTable innerJoin AuthorTable)
            .select(SeriesAuthorTable.series, AuthorTable.id, AuthorTable.name, AuthorTable.displayName)
            .where { SeriesAuthorTable.series inList ids }
            .groupBy({ it[SeriesAuthorTable.series].value }) {
                NamedId(it[AuthorTable.id].value, it[AuthorTable.displayName] ?: it[AuthorTable.name])
            }
    val genres =
        (GenreSeriesTable innerJoin GenresTable)
            .select(GenreSeriesTable.series, GenresTable.id, GenresTable.name)
            .where { GenreSeriesTable.series inList ids }
            .groupBy({ it[GenreSeriesTable.series].value }) {
                NamedId(it[GenresTable.id].value, it[GenresTable.name])
            }
    return rows.map { row ->
        Series(
            id = row.id,
            title = row.displayedTitle,
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
            genres = genres[row.id] ?: emptyList(),
        )
    }
}
