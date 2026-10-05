package io.thoth.server.database.tables

import io.thoth.server.database.extensions.timestampMillis
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import java.time.Instant
import java.util.UUID

sealed class LibraryEntityTable(
    table: String,
    nameColumn: String,
) : UUIDTable(table) {
    val library = reference("library_id", LibraryTable, onDelete = ReferenceOption.CASCADE).index()
    val deferDeletionUntil = timestampMillis("defer_deletion_until").nullable()
    val name = text(nameColumn, collate = "NOCASE")

    val visible get() = deferDeletionUntil.isNull()

    init {
        // deferDeletionUntil sits behind the sort column on purpose. In front of it SQLite seeks on both
        // constraints, but then has to sort the whole library whenever showInvisible drops the predicate.
        index(false, library, name, deferDeletionUntil)
    }
}

context(_: Transaction)
fun LibraryEntityTable.create(
    libraryId: UUID,
    name: String,
    deferDeletionUntil: Instant? = null,
): UUID =
    insertAndGetId {
        it[library] = libraryId
        it[this.name] = name
        it[this.deferDeletionUntil] = deferDeletionUntil
    }.value
