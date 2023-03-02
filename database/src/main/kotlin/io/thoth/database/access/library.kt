package io.thoth.database.access

import io.thoth.database.tables.Library
import io.thoth.models.LibraryModel

fun Library.toModel() =
    LibraryModel(
        id = id.value,
        name = name,
        icon = icon,
        scanIndex = scanIndex,
        preferEmbeddedMetadata = preferEmbeddedMetadata,
        folders = folders,
        metadataScanners = metadataScanners,
        fileScanners = fileScanners)
