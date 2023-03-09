package io.thoth.database.access

import io.thoth.database.tables.Library
import io.thoth.database.tables.TLibraries
import io.thoth.models.LibraryModel
import java.nio.file.Path
import java.util.*
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
    val potentialLibraries = all().filter { lib -> lib.folders.map { Path.of(it) }.any { path.contains(it) } }
    if (potentialLibraries.isEmpty()) return null
    if (potentialLibraries.size == 1) return potentialLibraries.first()
    log.error { "Multiple libraries match path $path" }
    return null
}

fun Library.Companion.allFolders(): List<Path> = all().flatMap { it.folders.map { Path.of(it) } }

fun Library.Companion.foldersOverlap(newLibId: UUID, folders: List<String>): Pair<Boolean, List<String>> {
    val newFolders = folders.map { Path.of(it) }
    val allFolders = find { TLibraries.id neq newLibId }.flatMap { it.folders }.map { Path.of(it) }
    val overlaps = newFolders.filter { newFolder -> allFolders.any { it.contains(newFolder) } }
    return Pair(overlaps.isEmpty(), overlaps.map { it.toString() })
}
