package io.thoth.server.api

import io.thoth.models.FileScanner
import io.thoth.models.MetadataAgent

data class LibraryApiModel(
    val name: String,
    val icon: String?,
    val folders: List<String>,
    val preferEmbeddedMetadata: Boolean,
    val metadataScanners: List<MetadataAgent>,
    val fileScanners: List<FileScanner>
) {
    init {
        require(folders.isNotEmpty())
        require(metadataScanners.isNotEmpty())
        require(fileScanners.isNotEmpty())
    }
}

data class PartialLibraryApiModel(
    val name: String?,
    val icon: String?,
    val folders: List<String>?,
    val preferEmbeddedMetadata: Boolean?,
    val metadataScanners: List<MetadataAgent>?,
    val fileScanners: List<FileScanner>?
) {
    init {
        require(folders == null || folders.isNotEmpty())
        require(metadataScanners == null || metadataScanners.isNotEmpty())
        require(fileScanners == null || fileScanners.isNotEmpty())
    }
}
