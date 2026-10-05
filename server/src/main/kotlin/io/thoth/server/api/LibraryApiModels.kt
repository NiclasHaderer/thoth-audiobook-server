package io.thoth.server.api

import io.ktor.server.routing.RoutingContext
import io.thoth.metadata.MetadataAgents
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import io.thoth.models.FileScanner
import io.thoth.models.NamedMetadataAgent
import io.thoth.openapi.ktor.ValidateObject
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.file.analyzer.AudioFileAnalyzers
import org.koin.ktor.ext.get
import java.util.Optional

data class UpdateLibrary(
    val name: String,
    val icon: String?,
    val folders: List<String>,
    val preferEmbeddedMetadata: Boolean,
    val metadataAgents: List<NamedMetadataAgent>,
    val combineMetadataAgentFields: Boolean,
    val fileScanners: List<FileScanner>,
    val combineFileScannerFields: Boolean,
    var language: MetadataLanguage,
    var region: MetadataRegion,
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        requireNotEmpty(folders, "folder")
        requireNotEmpty(fileScanners, "file scanner")
        requireRegistered(metadataAgents, fileScanners)
    }
}

data class PartialUpdateLibrary(
    val name: Optional<String> = Optional.empty(),
    val icon: Optional<String>? = Optional.empty(),
    val folders: Optional<List<String>> = Optional.empty(),
    val preferEmbeddedMetadata: Optional<Boolean> = Optional.empty(),
    val metadataAgents: Optional<List<NamedMetadataAgent>> = Optional.empty(),
    val combineMetadataAgentFields: Optional<Boolean> = Optional.empty(),
    val fileScanners: Optional<List<FileScanner>> = Optional.empty(),
    val combineFileScannerFields: Optional<Boolean> = Optional.empty(),
    val language: Optional<MetadataLanguage> = Optional.empty(),
    val region: Optional<MetadataRegion> = Optional.empty(),
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        folders.ifPresent { requireNotEmpty(it, "folder") }
        fileScanners.ifPresent { requireNotEmpty(it, "file scanner") }
        requireRegistered(metadataAgents.orElse(emptyList()), fileScanners.orElse(emptyList()))
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
