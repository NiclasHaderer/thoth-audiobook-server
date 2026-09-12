package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

sealed class SeriesMetadata(
    name: String,
) : IdTable<UUID>(name) {
    final override val id = reference("series", SeriesTable, onDelete = ReferenceOption.CASCADE)
    final override val primaryKey = PrimaryKey(id)

    val title = text("title").nullable()
    val totalBooks = integer("totalBooks").nullable()
    val primaryWorks = integer("primaryWorks").nullable()
    val description = text("description").nullable()
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val coverID = reference("cover", ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()
}

object SeriesFileMetadataTable : SeriesMetadata("SeriesFileMetadata")

object SeriesAgentMetadataTable : SeriesMetadata("SeriesAgentMetadata")

object SeriesUserMetadataTable : SeriesMetadata("SeriesUserMetadata")

data class SeriesMetadataRow(
    val series: UUID,
    val title: String? = null,
    val totalBooks: Int? = null,
    val primaryWorks: Int? = null,
    val description: String? = null,
    val provider: String? = null,
    val providerID: String? = null,
    val coverID: UUID? = null,
)

context(_: Transaction)
fun SeriesMetadata.layer(seriesId: UUID): SeriesMetadataRow =
    selectAll()
        .where { id eq seriesId }
        .firstOrNull()
        ?.toSeriesMetadataRow(this)
        ?: SeriesMetadataRow(series = seriesId)

private fun ResultRow.toSeriesMetadataRow(table: SeriesMetadata): SeriesMetadataRow =
    SeriesMetadataRow(
        series = this[table.id].value,
        title = this[table.title],
        totalBooks = this[table.totalBooks],
        primaryWorks = this[table.primaryWorks],
        description = this[table.description],
        provider = this[table.provider],
        providerID = this[table.providerID],
        coverID = this[table.coverID]?.value,
    )

// Nothing outside of this file may write a series layer.
context(_: Transaction)
fun SeriesMetadata.write(row: SeriesMetadataRow) {
    val updated = update({ id eq row.series }) { write(it, row) }
    if (updated == 0) insert { write(it, row) }
    reconcileSeries(row.series)
}

private fun SeriesMetadata.write(
    stmt: UpdateBuilder<*>,
    row: SeriesMetadataRow,
) {
    stmt[id] = row.series
    stmt[title] = row.title
    stmt[totalBooks] = row.totalBooks
    stmt[primaryWorks] = row.primaryWorks
    stmt[description] = row.description
    stmt[provider] = row.provider
    stmt[providerID] = row.providerID
    stmt[coverID] = row.coverID
}
