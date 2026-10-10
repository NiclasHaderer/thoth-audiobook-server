package io.thoth.server.database.tables

import io.thoth.models.TitledId
import io.thoth.models.Track
import io.thoth.server.database.extensions.timestampMillis
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.UUID

object TrackTable : UUIDTable("track") {
    val title = text("title")
    val durationMs = long("duration_ms")
    val fileModifiedAt = timestampMillis("file_modified_at")
    val path = text("path").uniqueIndex()
    val book = reference("book_id", BookTable, onDelete = ReferenceOption.CASCADE).index()
    val library = reference("library_id", LibraryTable, onDelete = ReferenceOption.CASCADE).index()
    val scanIndex = ulong("scan_index")
    val trackNr = integer("track_nr").nullable()
}

data class TrackRow(
    val id: UUID,
    val title: String,
    val durationMs: Long,
    val fileModifiedAt: Instant,
    val path: String,
    val book: UUID,
    val library: UUID,
    val scanIndex: ULong,
    val trackNr: Int?,
) {
    fun toModel(
        book: TitledId,
        trackNr: Int,
    ): Track =
        Track(
            id = id,
            title = title,
            trackNr = trackNr,
            durationMs = durationMs,
            fileModifiedAt = fileModifiedAt,
            book = book,
        )
}

fun ResultRow.toTrackRow(): TrackRow =
    TrackRow(
        id = this[TrackTable.id].value,
        title = this[TrackTable.title],
        durationMs = this[TrackTable.durationMs],
        fileModifiedAt = this[TrackTable.fileModifiedAt],
        path = this[TrackTable.path],
        book = this[TrackTable.book].value,
        library = this[TrackTable.library].value,
        scanIndex = this[TrackTable.scanIndex],
        trackNr = this[TrackTable.trackNr],
    )

context(_: Transaction)
fun TrackTable.insert(row: TrackRow): UUID {
    insert { write(it, row) }
    return row.id
}

context(_: Transaction)
fun TrackTable.update(row: TrackRow) {
    update({ TrackTable.id eq row.id }) { write(it, row) }
}

private fun write(
    stmt: UpdateBuilder<*>,
    row: TrackRow,
) {
    stmt[TrackTable.id] = row.id
    stmt[TrackTable.title] = row.title
    stmt[TrackTable.durationMs] = row.durationMs
    stmt[TrackTable.fileModifiedAt] = row.fileModifiedAt
    stmt[TrackTable.path] = row.path
    stmt[TrackTable.book] = row.book
    stmt[TrackTable.library] = row.library
    stmt[TrackTable.scanIndex] = row.scanIndex
    stmt[TrackTable.trackNr] = row.trackNr
}
