package io.thoth.database.tables.meta

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object TMetaSeries : UUIDTable("MetaSeries") {
    val title = varchar("title", 255)
    val provider = varchar("provider", 255)
    val itemID = varchar("itemID", 255)
    val totalBooks = integer("totalBooks").nullable()
    val primaryWorks = integer("primaryWorks").nullable()
    val cover = blob("cover").nullable()
    val description = text("description").nullable()
}

class MetaSeries(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MetaSeries>(TMetaSeries)

    var title by TMetaSeries.title
    var provider by TMetaSeries.provider
    var itemID by TMetaSeries.itemID
    var totalBooks by TMetaSeries.totalBooks
    var primaryWorks by TMetaSeries.primaryWorks
    var cover by TMetaSeries.cover
    var description by TMetaSeries.description
    val authors by MetaAuthor via TMetaSeriesAuthorMapping
    val books by MetaBook via TMetaSeriesBookMapping
    val genres by MetaGenre via TMetaGenreSeriesMapping
}
