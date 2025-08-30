package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.date
import java.util.UUID

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

class AuthorEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<AuthorEntity>(AuthorTable)

    var name by AuthorTable.name
    var displayName by AuthorTable.displayName
    var biography by AuthorTable.biography
    var imageID by AuthorTable.imageID
    var website by AuthorTable.website
    var birthDate by AuthorTable.birthDate
    var bornIn by AuthorTable.bornIn
    var deathDate by AuthorTable.deathDate

    // Provider
    var provider by AuthorTable.provider
    var providerID by AuthorTable.providerID

    // Relations
    var books by BookeEntity via AuthorBookTable
    var series by SeriesEntity via SeriesAuthorTable
    var library by LibraryEntity referencedOn AuthorTable.library
}
