package io.github.huiibuh.db.tables

import io.github.huiibuh.models.NamedId
import io.github.huiibuh.models.SeriesModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object TSeries : UUIDTable("Series") {
    val title = varchar("title", 250).uniqueIndex()
    val asin = char("asin", 10).nullable()
    val description = text("description").nullable()
    val author = reference("author", TAuthors)
}

class Series(id: EntityID<UUID>) : UUIDEntity(id), ToModel<SeriesModel> {
    companion object : UUIDEntityClass<Series>(TSeries)

    var title by TSeries.title
    var asin by TSeries.asin
    var description by TSeries.description
    var author by Author referencedOn TSeries.author

    override fun toModel(): SeriesModel {
        val books = Book.find { TBooks.series eq id.value }
        return SeriesModel(
            id = id.value,
            title = title,
            asin = asin,
            amount = books.count(),
            description = description,
            author = NamedId(
                name = author.name,
                id = author.id.value
            ),
            images = books.mapNotNull { it.cover?.id?.value }
        )
    }


}
