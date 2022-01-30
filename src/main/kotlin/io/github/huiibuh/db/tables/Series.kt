package io.github.huiibuh.db.tables

import io.github.huiibuh.api.exceptions.APINotFound
import io.github.huiibuh.db.ToModel
import io.github.huiibuh.extensions.findOne
import io.github.huiibuh.models.BookModel
import io.github.huiibuh.models.NamedId
import io.github.huiibuh.models.SeriesModel
import io.github.huiibuh.models.SeriesModelWithBooks
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
    val providerID = reference("providerID", TProviderID).nullable()
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

        @Throws(APINotFound::class)
        fun getById(uuid: UUID, order: SortOrder = SortOrder.ASC) = transaction {
            val series = Series.findById(uuid)?.toModel() ?: throw APINotFound("Could not find series")
            val books = Book.forSeries(uuid).sortedWith(compareBy(BookModel::year, BookModel::seriesIndex))
            val index = Series.all().orderBy(TSeries.title.lowerCase() to order).indexOfFirst { it.id.value === uuid }
            SeriesModelWithBooks.fromModel(series, books, index)
        }

        fun getByName(seriesTitle: String): Series? = transaction {
            Series.findOne { TSeries.title like seriesTitle }
        }
    }

    var title by TSeries.title
    var updateTime by TSeries.updateTime
    var providerID by ProviderID optionalReferencedOn TSeries.providerID
    var description by TSeries.description
    var author by Author referencedOn TSeries.author

    override fun toModel(): SeriesModel {
        val books = Book.find { TBooks.series eq id.value }
        return SeriesModel(
            id = id.value,
            title = title,
            providerID = providerID?.toModel(),
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
