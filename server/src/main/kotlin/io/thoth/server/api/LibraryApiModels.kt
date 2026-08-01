package io.thoth.server.api

import io.ktor.server.routing.RoutingContext
import io.thoth.metadata.MetadataAgents
import io.thoth.models.FileScanner
import io.thoth.models.NamedMetadataAgent
import io.thoth.openapi.ktor.ValidateObject
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.file.analyzer.AudioFileAnalyzers
import org.koin.ktor.ext.get

data class UpdateLibrary(
    val name: String,
    val icon: String?,
    val folders: List<String>,
    val preferEmbeddedMetadata: Boolean,
    val metadataAgents: List<NamedMetadataAgent>,
    val fileScanners: List<FileScanner>,
    var language: String,
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        requireNotEmpty(folders, "folder")
        requireNotEmpty(fileScanners, "file scanner")
        requireRegistered(metadataAgents, fileScanners)
    }
}

data class PartialUpdateLibrary(
    val name: String?,
    val icon: String?,
    val folders: List<String>?,
    val preferEmbeddedMetadata: Boolean?,
    val metadataAgents: List<NamedMetadataAgent>?,
    val fileScanners: List<FileScanner>?,
    val language: String?,
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        // An absent list means "leave unchanged", so it is skipped. An empty one is an explicit new value and
        // gets the same treatment as on a create.
        folders?.let { requireNotEmpty(it, "folder") }
        fileScanners?.let { requireNotEmpty(it, "file scanner") }
        requireRegistered(metadataAgents.orEmpty(), fileScanners.orEmpty())
    }
}

private fun requireNotEmpty(
    values: List<*>,
    what: String,
) {
    if (values.isEmpty()) throw ErrorResponse.userError("Library must have at least one $what")
}

private fun RoutingContext.requireRegistered(
    metadataAgents: List<NamedMetadataAgent>,
    fileScanners: List<FileScanner>,
) {
    requireKnownNames(
        kind = "metadata agent",
        requested = metadataAgents.map { it.name },
        available = call.application.get<MetadataAgents>().map { it.name },
    )
    requireKnownNames(
        kind = "file scanner",
        requested = fileScanners.map { it.name },
        available = call.application.get<AudioFileAnalyzers>().map { it.name },
    )
}

private fun requireKnownNames(
    kind: String,
    requested: List<String>,
    available: List<String>,
) {
    val unknown = requested.filterNot { it in available }.distinct()
    if (unknown.isNotEmpty()) {
        throw ErrorResponse.userError(
            "Unknown $kind: ${unknown.joinToString()}",
            mapOf("unknown" to unknown, "available" to available),
        )
    }
}
