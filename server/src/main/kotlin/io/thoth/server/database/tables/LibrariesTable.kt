package io.thoth.server.database.tables

import io.thoth.models.FileScanner
import io.thoth.models.Library
import io.thoth.models.NamedMetadataAgent
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.extensions.json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

object LibrariesTable : UUIDTable("Libraries") {
    val name = varchar("name", 255)
    val icon = text("icon").nullable()
    val scanIndex = ulong("scanIndex").default(0uL)
    val folders =
        json<List<String>>("folders") {
            if (it.isEmpty()) {
                throw ErrorResponse.userError("folders must have at least one element")
            }
        }
    val preferEmbeddedMetadata = bool("preferEmbeddedMetadata").default(false)
    // Deliberately unconstrained: a library with no agents just does no online metadata lookups.
    val metadataAgents = json<List<NamedMetadataAgent>>("metadataAgents")
    val fileScanners =
        json<List<FileScanner>>("fileScanners") {
            if (it.isEmpty()) {
                throw ErrorResponse.userError("fileScanners must have at least one element")
            }
        }

    // TODO make enum
    val language = varchar("language", 255)
}

data class LibraryRow(
    val id: UUID,
    val name: String,
    val icon: String?,
    val scanIndex: ULong,
    val folders: List<String>,
    val preferEmbeddedMetadata: Boolean,
    val metadataAgents: List<NamedMetadataAgent>,
    val fileScanners: List<FileScanner>,
    val language: String,
) {
    fun toModel(bookCount: Long): Library =
        Library(
            id = id,
            name = name,
            icon = icon,
            preferEmbeddedMetadata = preferEmbeddedMetadata,
            folders = folders,
            metadataAgents = metadataAgents,
            fileScanners = fileScanners,
            language = language,
            bookCount = bookCount,
        )
}

fun ResultRow.toLibraryRow(): LibraryRow =
    LibraryRow(
        id = this[LibrariesTable.id].value,
        name = this[LibrariesTable.name],
        icon = this[LibrariesTable.icon],
        scanIndex = this[LibrariesTable.scanIndex],
        folders = this[LibrariesTable.folders],
        preferEmbeddedMetadata = this[LibrariesTable.preferEmbeddedMetadata],
        metadataAgents = this[LibrariesTable.metadataAgents],
        fileScanners = this[LibrariesTable.fileScanners],
        language = this[LibrariesTable.language],
    )

context(_: Transaction)
fun LibrariesTable.insert(row: LibraryRow): UUID {
    insert { write(it, row) }
    return row.id
}

context(_: Transaction)
fun LibrariesTable.update(row: LibraryRow) {
    update({ LibrariesTable.id eq row.id }) { write(it, row) }
}

private fun write(
    stmt: UpdateBuilder<*>,
    row: LibraryRow,
) {
    stmt[LibrariesTable.id] = row.id
    stmt[LibrariesTable.name] = row.name
    stmt[LibrariesTable.icon] = row.icon
    stmt[LibrariesTable.scanIndex] = row.scanIndex
    stmt[LibrariesTable.folders] = row.folders
    stmt[LibrariesTable.preferEmbeddedMetadata] = row.preferEmbeddedMetadata
    stmt[LibrariesTable.metadataAgents] = row.metadataAgents
    stmt[LibrariesTable.fileScanners] = row.fileScanners
    stmt[LibrariesTable.language] = row.language
}
