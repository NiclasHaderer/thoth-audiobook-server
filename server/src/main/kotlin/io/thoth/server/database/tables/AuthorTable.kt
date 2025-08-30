package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.date

object AuthorTable : UUIDTable("Authors") {
    val name = varchar("name", 255)
    val displayName = varchar("displayName", 255).nullable()
    val biography = text("biography").nullable()
    val website = varchar("website", 255).nullable()
    val birthDate = date("birthDate").nullable()
    val bornIn = varchar("bornIn", 255).nullable()
    val deathDate = date("deathDate").nullable()

    // Provider
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()

    // Relations
    val imageID = reference("imageId", ImageTable, onDelete = ReferenceOption.CASCADE).nullable()
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE)
}
