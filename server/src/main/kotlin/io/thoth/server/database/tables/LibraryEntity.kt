package io.thoth.server.database.tables

import io.thoth.models.Library
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
    var metadataAgents by LibrariesTable.metadataAgents
    var fileScanners by LibrariesTable.fileScanners
    var language by LibrariesTable.language

    fun toModel(): Library =
        Library(
            id = id.value,
            name = name,
            icon = icon,
            scanIndex = scanIndex,
            preferEmbeddedMetadata = preferEmbeddedMetadata,
            folders = folders,
            metadataScanners = metadataAgents,
            fileScanners = fileScanners,
            language = language,
        )
}
