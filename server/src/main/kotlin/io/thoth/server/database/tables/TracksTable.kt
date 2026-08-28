package io.thoth.server.database.tables

import io.thoth.models.TitledId
import io.thoth.models.Track
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

object TracksTable : UUIDTable("Tracks") {
    val title = text("title")
    val durationMs = long("durationMs")
    val fileModifiedAt = long("fileModifiedAt")
    val path = text("path").uniqueIndex()
    val book = reference("book", BooksTable, onDelete = ReferenceOption.CASCADE).index()
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE).index()
    val scanIndex = ulong("scanIndex")
    val trackNr = integer("trackNr").nullable()
}

data class TrackRow(
    val id: UUID,
    val title: String,
    val durationMs: Long,
    val fileModifiedAt: Long,
    val path: String,
    val book: UUID,
    val library: UUID,
    val scanIndex: ULong,
    val trackNr: Int?,
) {
    fun toModel(book: TitledId): Track =
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
        id = this[TracksTable.id].value,
        title = this[TracksTable.title],
        durationMs = this[TracksTable.durationMs],
        fileModifiedAt = this[TracksTable.fileModifiedAt],
        path = this[TracksTable.path],
        book = this[TracksTable.book].value,
        library = this[TracksTable.library].value,
        scanIndex = this[TracksTable.scanIndex],
        trackNr = this[TracksTable.trackNr],
    )

context(_: Transaction)
fun TracksTable.insert(row: TrackRow): UUID {
    insert { write(it, row) }
    return row.id
}

context(_: Transaction)
fun TracksTable.update(row: TrackRow) {
    update({ TracksTable.id eq row.id }) { write(it, row) }
}

private fun write(
    stmt: UpdateBuilder<*>,
    row: TrackRow,
) {
    stmt[TracksTable.id] = row.id
    stmt[TracksTable.title] = row.title
    stmt[TracksTable.durationMs] = row.durationMs
    stmt[TracksTable.fileModifiedAt] = row.fileModifiedAt
    stmt[TracksTable.path] = row.path
    stmt[TracksTable.book] = row.book
    stmt[TracksTable.library] = row.library
    stmt[TracksTable.scanIndex] = row.scanIndex
    stmt[TracksTable.trackNr] = row.trackNr
}
