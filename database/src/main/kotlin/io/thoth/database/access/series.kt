package io.thoth.database.access

import io.thoth.common.extensions.findOne
import io.thoth.database.tables.Book
import io.thoth.database.tables.Series
import io.thoth.database.tables.TSeries
import io.thoth.models.BookModel
import io.thoth.models.NamedId
import io.thoth.models.SeriesModel
import io.thoth.models.SeriesModelWithBooks
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.lowerCase
import java.time.LocalDateTime
import java.util.*

fun Series.Companion.getMultiple(limit: Int, offset: Long, order: SortOrder = SortOrder.ASC): List<SeriesModel> {
    return Series.all().limit(limit, offset).orderBy(TSeries.title.lowerCase() to order).map {
        it.toModel()
    }
}

fun Series.Companion.count(): Long {
    return Series.all().count()
}


fun Series.Companion.getById(seriesId: UUID): SeriesModel? {
    return findById(seriesId)?.toModel() ?: return null
}


fun Series.Companion.getDetailedById(seriesId: UUID, bookOrder: SortOrder = SortOrder.ASC): SeriesModelWithBooks? {
    val series = getById(seriesId) ?: return null
    val books = Book.forSeries(seriesId).sortedWith(compareBy(BookModel::date, BookModel::seriesIndex))
    val index = all().orderBy(TSeries.title.lowerCase() to bookOrder).indexOfFirst { it.id.value == seriesId }
    return SeriesModelWithBooks.fromModel(series, books, index)
}

fun Series.Companion.findByName(seriesTitle: String): Series? {
    return findOne { TSeries.title like seriesTitle }
}

fun Series.toModel(): SeriesModel {

    return SeriesModel(
        id = id.value,
        title = title,
        amount = books.count(),
        description = description,
        // TODO
        updateTime = LocalDateTime.now(),
        authors = authors.map { NamedId(it.name, it.id.value) },
        images = books.mapNotNull { it.coverID?.value }.distinctBy { it }
    )
}
