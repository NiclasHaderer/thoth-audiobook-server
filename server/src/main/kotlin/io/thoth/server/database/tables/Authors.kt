package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.date
import java.util.UUID

object TAuthors : UUIDTable("Authors") {
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
    val imageID = reference("imageId", TImages, onDelete = ReferenceOption.CASCADE).nullable()
    val library = reference("library", TLibraries, onDelete = ReferenceOption.CASCADE)
}

class Author(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Author>(TAuthors)

    var name by TAuthors.name
    var displayName by TAuthors.displayName
    var biography by TAuthors.biography
    var imageID by TAuthors.imageID
    var website by TAuthors.website
    var birthDate by TAuthors.birthDate
    var bornIn by TAuthors.bornIn
    var deathDate by TAuthors.deathDate

    // Provider
    var provider by TAuthors.provider
    var providerID by TAuthors.providerID

    // Relations
    var books by Book via TAuthorBookMapping
    var series by Series via TSeriesAuthorMapping
    var library by Library referencedOn TAuthors.library
}
