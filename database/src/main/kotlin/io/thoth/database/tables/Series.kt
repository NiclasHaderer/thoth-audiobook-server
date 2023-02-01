package io.thoth.database.tables

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object TSeries : UUIDTable("Series") {
    val title = varchar("title", 255)
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val totalBooks = integer("totalBooks").nullable()
    val primaryWorks = integer("primaryWorks").nullable()
    val cover = reference("cover", TImages).nullable()
    val description = text("description").nullable()
}

class Series(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Series>(TSeries)

    var title by TSeries.title
    var provider by TSeries.provider
    var providerID by TSeries.providerID
    var totalBooks by TSeries.totalBooks
    var primaryWorks by TSeries.primaryWorks
    var cover by TSeries.cover
    var description by TSeries.description
    var authors by Author via TSeriesAuthorMapping
    var books by Book via TSeriesBookMapping
    var genres by Genre via TGenreSeriesMapping
}
