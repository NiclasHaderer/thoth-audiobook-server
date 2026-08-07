package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.Coalesce
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.date

object AuthorTable : UUIDTable("Authors") {
    val name = text("name")
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
    val imageID = reference("imageId", ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE).index()

    val displayedName = Coalesce(displayName, name)
}
