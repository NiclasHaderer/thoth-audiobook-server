package io.thoth.models

import java.util.*

data class MetadataAgent(
    var name: String,
    var countryCode: Locale.IsoCountryCode
)

data class FileScanner(
    var name: String,
    var folders: List<String>
)

class LibraryModel(
    val id: UUID,
    val name: String,
    val icon: String?,
    val scanIndex: ULong,
    val preferEmbeddedMetadata: Boolean,
    val folders: List<String>,
    val metadataScanners: List<MetadataAgent>,
    val fileScanners: List<FileScanner>
)