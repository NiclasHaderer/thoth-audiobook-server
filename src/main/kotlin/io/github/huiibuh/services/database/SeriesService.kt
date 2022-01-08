package io.github.huiibuh.services.database

import io.github.huiibuh.api.exceptions.APINotFound
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.db.tables.TSeries
import io.github.huiibuh.models.BookModel
import io.github.huiibuh.models.SeriesModelWithBooks
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object SeriesService {

    fun getMultiple(limit: Int, offset: Long, order: SortOrder = SortOrder.ASC) = transaction {
        Series.all().limit(limit, offset).orderBy(TSeries.title to order).map {
            it.toModel()
        }
    }

    fun get(id: UUID, seriesOrder: SortOrder = SortOrder.ASC) = transaction {
        val series = Series.findById(id)?.toModel() ?: throw APINotFound("Could not find series")
        val books = BookService.forSeries(id).sortedWith(compareBy(BookModel::year, BookModel::seriesIndex))
        val index = Series.all().orderBy(TSeries.title to seriesOrder).indexOfFirst { it.id.value === id }
        SeriesModelWithBooks.fromModel(series, books, index)
    }
}
