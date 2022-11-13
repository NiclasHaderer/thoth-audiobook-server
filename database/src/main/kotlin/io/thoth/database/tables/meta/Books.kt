package io.thoth.database.tables.meta

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object TMetaBooks : UUIDTable("MetaBooks") {
    val title = varchar("title", 255)
    val provider = varchar("provider", 255)
    val itemID = varchar("itemID", 255)
    val year = integer("year").nullable()
    val language = varchar("language", 255).nullable()
    val description = text("description").nullable()
    val narrator = varchar("name", 255).nullable()
    val cover = blob("cover").nullable()
    val rating = float("rating").nullable()
}


class MetaBook(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MetaBook>(TMetaBooks)

    var title by TMetaBooks.title
    var provider by TMetaBooks.provider
    var itemID by TMetaBooks.itemID
    var year by TMetaBooks.year
    var language by TMetaBooks.language
    var description by TMetaBooks.description
    var narrator by TMetaBooks.narrator
    var cover by TMetaBooks.cover
    var rating by TMetaBooks.rating
    val authors by MetaAuthor via TMetaAuthorBookMapping
    val series by MetaSeries via TMetaSeriesBookMapping
    val genres by MetaGenre via TMetaGenreBookMapping
}
