package io.thoth.database.access

import io.thoth.database.tables.Library
import io.thoth.models.LibraryModel
import java.nio.file.Path
import mu.KotlinLogging.logger

private val log = logger {}

fun Library.toModel() =
    LibraryModel(
        id = id.value,
        name = name,
        icon = icon,
        scanIndex = scanIndex,
        preferEmbeddedMetadata = preferEmbeddedMetadata,
        folders = folders,
        metadataScanners = metadataScanners,
        fileScanners = fileScanners,
    )

fun Library.Companion.getMatching(path: Path): Library? {
    val libraries = all()
    val potentialLibraries = libraries.filter { lib -> lib.folders.map { Path.of(it) }.any { path.contains(it) } }
    if (potentialLibraries.isEmpty()) return null
    if (potentialLibraries.size == 1) return potentialLibraries.first()
    log.error { "Multiple libraries match path $path" }
    return null
}

fun Library.Companion.allFolders(): List<Path> = all().flatMap { it.folders.map { Path.of(it) } }
