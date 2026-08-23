package io.thoth.models

import java.util.UUID

data class Library(
    val id: UUID,
    val name: String,
    val icon: String?,
    val preferEmbeddedMetadata: Boolean,
    val folders: List<String>,
    val metadataAgents: List<NamedMetadataAgent>,
    val fileScanners: List<FileScanner>,
    val language: String,
    val bookCount: Long,
)
