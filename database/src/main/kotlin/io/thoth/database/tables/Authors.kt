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
    val imageId = reference("imageId", TImages, onDelete = ReferenceOption.CASCADE).nullable()
    val website = varchar("website", 255).nullable()
    val born = date("born").nullable()
    val bornIn = varchar("bornIn", 255).nullable()
    val died = date("died").nullable()
}

class Author(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Author>(TAuthors)

    var name by TAuthors.name
    var provider by TAuthors.provider
    var providerID by TAuthors.providerID
    var biography by TAuthors.biography
    var imageID by TAuthors.imageId
    var website by TAuthors.website
    var born by TAuthors.born
    var bornIn by TAuthors.bornIn
    var died by TAuthors.died
    val books by Book via TAuthorBookMapping
    val series by Series via TSeriesAuthorMapping
    val genres by Genre via TGenreAuthorMapping
}

