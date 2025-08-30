package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class LibraryEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<LibraryEntity>(LibrariesTable)

    var name by LibrariesTable.name
    var icon by LibrariesTable.icon
    var scanIndex by LibrariesTable.scanIndex
    var folders by LibrariesTable.folders
    var preferEmbeddedMetadata by LibrariesTable.preferEmbeddedMetadata
    var metadataScanners by LibrariesTable.metadataScanners
    var fileScanners by LibrariesTable.fileScanners
    var language by LibrariesTable.language
}
