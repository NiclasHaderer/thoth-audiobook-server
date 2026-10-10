package io.thoth.server.database.tables

import io.thoth.server.database.extensions.timestampMillis
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import java.time.Instant
import java.util.UUID

interface MetadataField {
    val name: String

    val column: String get() = name.lowercase()
}

sealed class LibraryEntityTable(
    table: String,
    nameColumn: String,
) : UUIDTable(table) {
    val library = reference("library_id", LibraryTable, onDelete = ReferenceOption.CASCADE).index()
    val deferDeletionUntil = timestampMillis("defer_deletion_until").nullable()
    val name = text(nameColumn, collate = "NOCASE")

    // What the files called the entity when a scan created it, so one that a user or a metadata agent renamed is
    // still recognised by the next file that carries the old name. Never rewritten: a file that joined through the
    // current name would otherwise replace it, and the files that carry the old name would split off on rescan.
    val taggedName = text("tagged_name", collate = "NOCASE").nullable()

    val visible get() = deferDeletionUntil.isNull()

    init {
        // deferDeletionUntil sits behind the sort column on purpose. In front of it SQLite seeks on both
        // constraints, but then has to sort the whole library whenever showInvisible drops the predicate.
        index(false, library, name, deferDeletionUntil)
        index(false, library, taggedName)
    }
}

context(_: Transaction)
fun LibraryEntityTable.create(
    libraryId: UUID,
    name: String,
    taggedName: String? = null,
    deferDeletionUntil: Instant? = null,
): UUID =
    insertAndGetId {
        it[library] = libraryId
        it[this.name] = name
        it[this.taggedName] = taggedName
        it[this.deferDeletionUntil] = deferDeletionUntil
    }.value
