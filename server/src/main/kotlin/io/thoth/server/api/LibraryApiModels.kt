package io.thoth.server.api

import io.thoth.models.FileScanner
import io.thoth.models.MetadataAgent

data class LibraryApiModel(
    val name: String,
    val icon: String?,
    val folders: List<String>,
    val preferEmbeddedMetadata: Boolean,
    val metadataScanners: List<MetadataAgent>,
    val fileScanners: List<FileScanner>,
    var language: String
) {
    init {
        require(folders.isNotEmpty())
        require(metadataScanners.isNotEmpty())
        require(fileScanners.isNotEmpty())
        // TODO verify that the metadataScanner exists
        // TODO verify that the fileScanner exists
    }
}

data class PartialLibraryApiModel(
    val name: String?,
    val icon: String?,
    val folders: List<String>?,
    val preferEmbeddedMetadata: Boolean?,
    val metadataScanners: List<MetadataAgent>?,
    val fileScanners: List<FileScanner>?,
    val language: String?
) {
    init {
        require(folders == null || folders.isNotEmpty())
        require(metadataScanners == null || metadataScanners.isNotEmpty())
        require(fileScanners == null || fileScanners.isNotEmpty())
        // TODO verify that the metadataScanner exists
        // TODO verify that the fileScanner exists
    }
}
