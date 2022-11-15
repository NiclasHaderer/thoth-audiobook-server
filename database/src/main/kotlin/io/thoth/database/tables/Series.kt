package io.thoth.database.tables

import io.thoth.common.extensions.findOne
import io.thoth.database.ToModel
import io.thoth.models.BookModel
import io.thoth.models.NamedId
import io.thoth.models.SeriesModel
import io.thoth.models.SeriesModelWithBooks
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

object TSeries : UUIDTable("Series") {
    val title = varchar("title", 250).uniqueIndex()
    val author = reference("author", TAuthors)
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val description = text("description").nullable()
}

class Series(id: EntityID<UUID>) : UUIDEntity(id), ToModel<SeriesModel> {
    companion object : UUIDEntityClass<Series>(TSeries) {
        fun removeUnused() = transaction {
            all().forEach {
                if (Book.find { TBooks.series eq it.id.value }.empty()) {
                    it.delete()
                }
            }
        }

        fun getMultiple(limit: Int, offset: Long, order: SortOrder = SortOrder.ASC) = transaction {
            Series.all().limit(limit, offset).orderBy(TSeries.title.lowerCase() to order).map {
                it.toModel()
            }
        }

        fun totalCount() = transaction { Series.all().count() }

        fun getById(uuid: UUID, order: SortOrder = SortOrder.ASC) = transaction {
            val series = Series.findById(uuid)?.toModel() ?: return@transaction null
            val books = Book.forSeries(uuid).sortedWith(compareBy(BookModel::date, BookModel::seriesIndex))
            val index = Series.all().orderBy(TSeries.title.lowerCase() to order).indexOfFirst { it.id.value === uuid }
            SeriesModelWithBooks.fromModel(series, books, index)
        }

        fun getByName(seriesTitle: String): Series? = transaction {
            Series.findOne { TSeries.title like seriesTitle }
        }
    }

    var title by TSeries.title
    var updateTime by TSeries.updateTime
    var description by TSeries.description
    var author by Author referencedOn TSeries.author

    override fun toModel(): SeriesModel {
        val books = Book.find { TBooks.series eq id.value }
        return SeriesModel(
            id = id.value,
            title = title,
            amount = books.count(),
            description = description,
            updateTime = updateTime,
            author = NamedId(
                name = author.name,
                id = author.id.value
            ),
            images = books.mapNotNull { it.cover?.id?.value }
        )
    }


}
