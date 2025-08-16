package io.thoth.server.database.access

import io.thoth.models.LibraryModel
import io.thoth.server.database.tables.Library

fun Library.toModel(): LibraryModel =
    LibraryModel(
        id = id.value,
        name = name,
        icon = icon,
        scanIndex = scanIndex,
        preferEmbeddedMetadata = preferEmbeddedMetadata,
        folders = folders,
        metadataScanners = metadataScanners,
        fileScanners = fileScanners,
        language = language,
    )
