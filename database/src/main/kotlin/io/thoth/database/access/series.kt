package io.thoth.database.access

import io.thoth.common.extensions.findOne
import io.thoth.database.tables.Series
import io.thoth.database.tables.TAuthors
import io.thoth.database.tables.TBooks
import io.thoth.database.tables.TSeries
import io.thoth.models.DetailedSeriesModel
import io.thoth.models.NamedId
import io.thoth.models.SeriesModel
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import java.util.*

fun Series.Companion.getMultiple(limit: Int, offset: Long, order: SortOrder = SortOrder.ASC): List<SeriesModel> {
    return Series.all().limit(limit, offset).orderBy(TSeries.title.lowerCase() to order).map {
        it.toModel()
    }
}

fun Series.Companion.getDetailedById(seriesId: UUID, order: SortOrder = SortOrder.ASC): DetailedSeriesModel? {
    val series = findById(seriesId) ?: return null
    return DetailedSeriesModel.fromModel(
        series = series.toModel(),
        books = series.books.orderBy(TBooks.releaseDate to order).map { it.toModel() },
    )
}

fun Series.Companion.positionOf(seriesId: UUID, order: SortOrder = SortOrder.ASC): Long? {
    val series = findById(seriesId) ?: return null
    return TSeries.select { TSeries.title.lowerCase() less series.title.lowercase() }
        .orderBy(TSeries.title.lowerCase() to order).count()
}

fun Series.Companion.findByName(seriesTitle: String): Series? {
    return findOne { TSeries.title like seriesTitle }
}

fun Series.toModel(order: SortOrder = SortOrder.ASC): SeriesModel {

    return SeriesModel(
        id = id.value,
        title = title,
        description = description,
        providerID = providerID,
        provider = provider,
        coverID = coverID?.value,
        primaryWorks = primaryWorks,
        totalBooks = totalBooks,
        authors = authors.orderBy(TAuthors.name.lowerCase() to order).map { NamedId(it.id.value, it.name) },
    )
}
