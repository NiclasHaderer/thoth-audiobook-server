package io.thoth.database.tables

import io.thoth.database.tables.meta.MetaSeries
import io.thoth.database.tables.meta.TMetaSeries
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.*

object TSeries : UUIDTable("Series") {
    val title = varchar("title", 250).uniqueIndex()
    val author = reference("author", TAuthors)
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val description = text("description").nullable()
    val linkedSeries = reference("linkedSeries", TMetaSeries).nullable()
}

class Series(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Series>(TSeries)

    var title by TSeries.title
    var updateTime by TSeries.updateTime
    var description by TSeries.description
    var author by Author referencedOn TSeries.author
    var linkedSeries by MetaSeries optionalReferencedOn TSeries.linkedSeries
}
