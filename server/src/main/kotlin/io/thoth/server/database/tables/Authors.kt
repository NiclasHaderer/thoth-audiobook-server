package io.thoth.server.database.tables

import java.util.*
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date

object TAuthors : UUIDTable("Authors") {
    val name = varchar("name", 255)
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

class Author(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Author>(TAuthors)

    var name by TAuthors.name
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
