package io.thoth.server.database.tables

import io.thoth.server.database.extensions.json
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

enum class SeriesField : LayerField {
    TITLE,
    PROVIDER,
    PROVIDER_ID,
    TOTAL_BOOKS,
    PRIMARY_WORKS,
    COVER_ID,
    DESCRIPTION,
}

sealed class SeriesMetadata(
    name: String,
) : IdTable<UUID>(name) {
    final override val id = reference("series_id", SeriesTable, onDelete = ReferenceOption.CASCADE)
    final override val primaryKey = PrimaryKey(id)

    val title = text(SeriesField.TITLE.column).nullable()
    val totalBooks = integer(SeriesField.TOTAL_BOOKS.column).nullable()
    val primaryWorks = integer(SeriesField.PRIMARY_WORKS.column).nullable()
    val description = text(SeriesField.DESCRIPTION.column).nullable()
    val provider = varchar(SeriesField.PROVIDER.column, 255).nullable()
    val providerId = varchar(SeriesField.PROVIDER_ID.column, 255).nullable()
    val coverId = reference(SeriesField.COVER_ID.column, ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val claimed = json<Set<SeriesField>>("claimed").default(emptySet())
}

object SeriesFileMetadataTable : SeriesMetadata("series_file_metadata")

object SeriesAgentMetadataTable : SeriesMetadata("series_agent_metadata")

object SeriesUserMetadataTable : SeriesMetadata("series_user_metadata")

data class SeriesMetadataRow(
    val series: UUID,
    val title: String? = null,
    val totalBooks: Int? = null,
    val primaryWorks: Int? = null,
    val description: String? = null,
    val provider: String? = null,
    val providerID: String? = null,
    val coverID: UUID? = null,
    override val claimed: Set<SeriesField> = emptySet(),
) : LayerRow<SeriesField>

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
        providerID = this[table.providerId],
        coverID = this[table.coverId]?.value,
        claimed = this[table.claimed],
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
    stmt[providerId] = row.providerID
    stmt[coverId] = row.coverID
    stmt[claimed] = row.claimed
}
