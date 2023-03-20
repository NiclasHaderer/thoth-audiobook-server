package io.thoth.database.tables

import java.util.*
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object TSeries : UUIDTable("Series") {
    val title = varchar("title", 255)
    val totalBooks = integer("totalBooks").nullable()
    val primaryWorks = integer("primaryWorks").nullable()
    val coverID = reference("cover", TImages).nullable()
    val description = text("description").nullable()
    val library = reference("library", TLibraries, onDelete = ReferenceOption.CASCADE)
    val seriesMeta = reference("seriesMeta", TSeriesMeta, onDelete = ReferenceOption.CASCADE)
}

class Series(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Series>(TSeries)

    var title by TSeries.title
    var totalBooks by TSeries.totalBooks
    var primaryWorks by TSeries.primaryWorks
    var coverID by TSeries.coverID
    var description by TSeries.description
    var authors by Author via TSeriesAuthorMapping
    var books by Book via TSeriesBookMapping
    var genres by Genre via TGenreSeriesMapping
    val library by Library referencedOn TSeries.library
    val seriesMeta by SeriesMeta referencedOn TSeries.seriesMeta
}

object TSeriesMeta : UUIDTable("SeriesMeta") {
    val title = varchar("title", 255).nullable()
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val totalBooks = integer("totalBooks").nullable()
    val primaryWorks = integer("primaryWorks").nullable()
    val coverID = reference("cover", TImages).nullable()
    val description = text("description").nullable()
}

class SeriesMeta(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SeriesMeta>(TSeriesMeta)

    var title by TSeriesMeta.title
    var provider by TSeriesMeta.provider
    var providerID by TSeriesMeta.providerID
    var totalBooks by TSeriesMeta.totalBooks
    var primaryWorks by TSeriesMeta.primaryWorks
    var coverID by TSeriesMeta.coverID
    var description by TSeriesMeta.description
}
