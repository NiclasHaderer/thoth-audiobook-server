package io.github.huiibuh.db.tables

import io.github.huiibuh.models.SeriesModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object TSeries : UUIDTable("series") {
    val title = varchar("title", 250).uniqueIndex()
    val asin = char("asin", 10).uniqueIndex().nullable()
    val description = text("description").nullable()
    val author = reference("author", TAuthors)
}

class Series(id: EntityID<UUID>) : UUIDEntity(id), ToModel<SeriesModel> {
    companion object : UUIDEntityClass<Series>(TSeries)

    private val authorID by TSeries.author

    var title by TSeries.title
    var asin by TSeries.asin
    var description by TSeries.description
    var author by Author referencedOn TSeries.author

    override fun toModel() = SeriesModel(
        id = id.value,
        name = title,
        asin = asin,
        description = description,
        author = authorID.value
    )
}
