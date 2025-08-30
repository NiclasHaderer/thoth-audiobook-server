package io.thoth.server.api

import io.ktor.server.routing.RoutingContext
import io.thoth.models.FileScanner
import io.thoth.models.MetadataAgent
import io.thoth.openapi.ktor.ValidateObject
import io.thoth.openapi.ktor.errors.ErrorResponse

data class UpdateLibrary(
    val name: String,
    val icon: String?,
    val folders: List<String>,
    val preferEmbeddedMetadata: Boolean,
    val metadataScanners: List<MetadataAgent>,
    val fileScanners: List<FileScanner>,
    var language: String,
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        if (folders.isEmpty()) {
            throw ErrorResponse.userError("Library must have at least one folder")
        }
        if (metadataScanners.isEmpty()) {
            throw ErrorResponse.userError("Library must have at least one metadata scanner")
        }
        if (fileScanners.isEmpty()) {
            throw ErrorResponse.userError("Library must have at least one file scanner")
        }
        // TODO verify that the metadataScanner exists
        // TODO verify that the fileScanner exists
    }
}

data class PartialUpdateLibrary(
    val name: String?,
    val icon: String?,
    val folders: List<String>?,
    val preferEmbeddedMetadata: Boolean?,
    val metadataScanners: List<MetadataAgent>?,
    val fileScanners: List<FileScanner>?,
    val language: String?,
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        if (folders?.isEmpty() == true) {
            throw ErrorResponse.userError("Library must have at least one folder")
        }
        if (metadataScanners?.isEmpty() == true) {
            throw ErrorResponse.userError("Library must have at least one metadata scanner")
        }
        if (fileScanners?.isEmpty() == true) {
            throw ErrorResponse.userError("Library must have at least one file scanner")
        }
        // TODO verify that the metadataScanner exists
        // TODO verify that the fileScanner exists
    }
}
