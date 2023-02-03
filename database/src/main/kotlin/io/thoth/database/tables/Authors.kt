package io.thoth.database.tables

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date
import java.util.*

object TAuthors : UUIDTable("Authors") {
    val name = varchar("name", 255)
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val biography = text("biography").nullable()
    val imageID = reference("imageId", TImages, onDelete = ReferenceOption.CASCADE).nullable()
    val website = varchar("website", 255).nullable()
    val birthDate = date("birthDate").nullable()
    val bornIn = varchar("bornIn", 255).nullable()
    val deathDate = date("deathDate").nullable()
}

class Author(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Author>(TAuthors)

    var name by TAuthors.name
    var provider by TAuthors.provider
    var providerID by TAuthors.providerID
    var biography by TAuthors.biography
    var imageID by TAuthors.imageID
    var website by TAuthors.website
    var birthDate by TAuthors.birthDate
    var bornIn by TAuthors.bornIn
    var deathDate by TAuthors.deathDate
    val books by Book via TAuthorBookMapping
    val series by Series via TSeriesAuthorMapping
    val genres by Genre via TGenreAuthorMapping
}

