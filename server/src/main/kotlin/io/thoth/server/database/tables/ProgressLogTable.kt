package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.jdbc.insert
import io.thoth.server.database.extensions.timestampMillis
import java.time.Instant
import java.util.UUID

/**
 * Append-only: one row per progress write, never updated
 */
object ProgressLogTable : UUIDTable("ProgressLog") {
    val user = reference("user", UsersTable, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", BooksTable, onDelete = ReferenceOption.CASCADE)
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE).index()
    val positionMs = long("positionMs")
    val at = timestampMillis("at")

    init {
        index(false, user, at)
        index(false, user, book)
    }
}

data class ProgressLogRow(
    val id: UUID,
    val user: UUID,
    val book: UUID,
    val library: UUID,
    val positionMs: Long,
    val at: Instant,
)

fun ResultRow.toProgressLogRow(): ProgressLogRow =
    ProgressLogRow(
        id = this[ProgressLogTable.id].value,
        user = this[ProgressLogTable.user].value,
        book = this[ProgressLogTable.book].value,
        library = this[ProgressLogTable.library].value,
        positionMs = this[ProgressLogTable.positionMs],
        at = this[ProgressLogTable.at],
    )

context(_: Transaction)
fun ProgressLogTable.insert(row: ProgressLogRow): UUID {
    insert {
        it[id] = row.id
        it[user] = row.user
        it[book] = row.book
        it[library] = row.library
        it[positionMs] = row.positionMs
        it[at] = row.at
    }
    return row.id
}
