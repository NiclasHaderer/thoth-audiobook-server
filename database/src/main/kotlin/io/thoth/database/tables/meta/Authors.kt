package io.thoth.database.tables.meta

import io.thoth.database.tables.TImages
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date
import java.util.*

object TMetaAuthors : UUIDTable("MetaAuthors") {
    val name = varchar("name", 255)
    val provider = varchar("provider", 255)
    val itemID = varchar("itemID", 255)
    val biography = text("biography").nullable()
    val imageId = reference("imageId", TImages, onDelete = ReferenceOption.CASCADE).nullable()
    val website = varchar("website", 255).nullable()
    val born = date("born").nullable()
    val bornIn = varchar("bornIn", 255).nullable()
    val died = date("died").nullable()
}

class MetaAuthor(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MetaAuthor>(TMetaAuthors)

    var name by TMetaAuthors.name
    var provider by TMetaAuthors.provider
    var itemID by TMetaAuthors.itemID
    var biography by TMetaAuthors.biography
    var imageId by TMetaAuthors.imageId
    var website by TMetaAuthors.website
    var born by TMetaAuthors.born
    var bornIn by TMetaAuthors.bornIn
    var died by TMetaAuthors.died
    val books by MetaBook via TMetaAuthorBookMapping
    val series by MetaSeries via TMetaSeriesAuthorMapping
    val genres by MetaGenre via TMetaGenreAuthorMapping
}

