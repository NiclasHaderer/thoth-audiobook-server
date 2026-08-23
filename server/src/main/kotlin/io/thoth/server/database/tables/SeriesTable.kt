package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import java.util.UUID

object SeriesTable : UUIDTable("Series") {
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE).index()
    val deferDeletionUntil = long("deferDeletionUntil").nullable()
}

context(_: Transaction)
fun SeriesTable.create(
    libraryId: UUID,
    deferDeletionUntil: Long? = null,
): UUID =
    insertAndGetId {
        it[library] = libraryId
        it[this.deferDeletionUntil] = deferDeletionUntil
    }.value
