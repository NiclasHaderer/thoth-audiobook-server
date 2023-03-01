package io.thoth.database.tables

import io.thoth.database.extensions.json
import io.thoth.models.FileScanner
import io.thoth.models.MetadataAgent
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import java.util.*


@OptIn(ExperimentalUnsignedTypes::class)
object TLibraries : UUIDTable("Libraries") {
    val name = varchar("name", 255)
    val icon = text("icon").nullable()
    val scanIndex = ulong("scanIndex").default(0uL)
    val preferEmbeddedMetadata = bool("preferEmbeddedMetadata").default(false)
    val folders = json<List<String>>("folders") { require(it.isNotEmpty()) }
    val metadataScanners = json<List<MetadataAgent>>("metadataScanners") { require(it.isNotEmpty()) }
    val fileScanners = json<List<FileScanner>>("fileScanners") { require(it.isNotEmpty()) }
}

class Library(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Library>(TLibraries)

    var name by TLibraries.name
    var icon by TLibraries.icon
    var scanIndex by TLibraries.scanIndex
    var folders by TLibraries.folders
    var preferEmbeddedMetadata by TLibraries.preferEmbeddedMetadata
    var metadataScanners by TLibraries.metadataScanners
    var fileScanners by TLibraries.fileScanners
}