package io.thoth.models

import java.util.UUID

data class MetadataAgent(
    var name: String,
)

data class FileScanner(
    var name: String,
)

data class Library(
    val id: UUID,
    val name: String,
    val icon: String?,
    val scanIndex: ULong,
    val preferEmbeddedMetadata: Boolean,
    val folders: List<String>,
    val metadataScanners: List<MetadataAgent>,
    val fileScanners: List<FileScanner>,
    var language: String,
)
