package io.thoth.server.database.tables

import io.thoth.models.FileScanner
import io.thoth.models.MetadataAgent
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.extensions.json
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

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
