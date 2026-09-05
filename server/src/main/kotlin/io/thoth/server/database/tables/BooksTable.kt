package io.thoth.server.database.tables

import io.thoth.server.database.extensions.timestampMillis
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import java.util.UUID

object BooksTable : UUIDTable("Books") {
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE).index()
    val deferDeletionUntil = timestampMillis("deferDeletionUntil").nullable()
}

context(_: Transaction)
fun BooksTable.create(libraryId: UUID): UUID = insertAndGetId { it[library] = libraryId }.value
