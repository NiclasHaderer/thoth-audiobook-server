package io.thoth.server.database.tables

import io.thoth.models.FileScanner
import io.thoth.models.MetadataAgent
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.extensions.json
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

object TLibraries : UUIDTable("Libraries") {
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
    val metadataScanners =
        json<List<MetadataAgent>>("metadataScanners") {
            if (it.isEmpty()) {
                throw ErrorResponse.userError("metadataScanners must have at least one element")
            }
        }
    val fileScanners =
        json<List<FileScanner>>("fileScanners") {
            if (it.isEmpty()) {
                throw ErrorResponse.userError("fileScanners must have at least one element")
            }
        }

    // TODO make enum
    val language = varchar("language", 255)
}

class Library(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Library>(TLibraries)

    var name by TLibraries.name
    var icon by TLibraries.icon
    var scanIndex by TLibraries.scanIndex
    var folders by TLibraries.folders
    var preferEmbeddedMetadata by TLibraries.preferEmbeddedMetadata
    var metadataScanners by TLibraries.metadataScanners
    var fileScanners by TLibraries.fileScanners
    var language by TLibraries.language
}
