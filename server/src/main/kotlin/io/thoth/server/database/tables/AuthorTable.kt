package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import java.util.UUID

object AuthorTable : UUIDTable("Authors") {
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE).index()
}

context(_: Transaction)
fun AuthorTable.create(libraryId: UUID): UUID = insertAndGetId { it[library] = libraryId }.value
