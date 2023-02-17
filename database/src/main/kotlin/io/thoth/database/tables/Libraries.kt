package io.thoth.database.tables

import io.thoth.database.extensions.json
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import java.util.*
import java.util.Locale.IsoCountryCode

data class MetadataAgent(
    var name: String,
    var countryCode: IsoCountryCode
)

data class FileScanner(
    var name: String,
    var folders: List<String>
)


@OptIn(ExperimentalUnsignedTypes::class)
object TLibraries : UUIDTable("Libraries") {
    val name = varchar("name", 255)
    val icon = text("icon").nullable()
    val scanIndex = ulong("scanIndex").default(0uL)
    val folders = text("folders").nullable()
    val preferEmbeddedMetadata = bool("preferEmbeddedMetadata").default(false)
    val metadataScanners = json<MetadataAgent>("metadataScanners").nullable()
    val fileScanners = json<List<FileScanner>>("fileScanners"){ require(it.isNotEmpty()) }
}

class Libraries(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Libraries>(TLibraries)

    var name by TLibraries.name
    var icon by TLibraries.icon
    var scanIndex by TLibraries.scanIndex
    var folders by TLibraries.folders
    var preferEmbeddedMetadata by TLibraries.preferEmbeddedMetadata
    var metadataScanners by TLibraries.metadataScanners
    var fileScanners by TLibraries.fileScanners
}