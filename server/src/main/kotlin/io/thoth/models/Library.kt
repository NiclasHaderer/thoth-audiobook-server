package io.thoth.models

import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import java.util.UUID

data class Library(
    val id: UUID,
    val name: String,
    val icon: String?,
    val preferEmbeddedMetadata: Boolean,
    val folders: List<String>,
    val metadataAgents: List<NamedMetadataAgent>,
    val combineMetadataAgentFields: Boolean,
    val fileScanners: List<FileScanner>,
    val combineFileScannerFields: Boolean,
    val language: MetadataLanguage,
    val region: MetadataRegion,
    val bookCount: Long,
)
