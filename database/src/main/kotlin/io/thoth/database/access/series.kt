package io.thoth.database.access

import io.thoth.common.extensions.findOne
import io.thoth.database.tables.Series
import io.thoth.database.tables.TAuthors
import io.thoth.database.tables.TBooks
import io.thoth.database.tables.TSeries
import io.thoth.models.NamedId
import io.thoth.models.SeriesModel
import io.thoth.models.SeriesModelWithBooks
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.lowerCase
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


fun Series.Companion.getDetailedById(seriesId: UUID, order: SortOrder = SortOrder.ASC): SeriesModelWithBooks? {
    val series = findById(seriesId) ?: return null
    return SeriesModelWithBooks.fromModel(
        series = series.toModel(),
        books = series.books.orderBy(TBooks.releaseDate to order).map { it.toModel() },
    )
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
        authors = authors.orderBy(TAuthors.name.lowerCase() to order).map { NamedId(it.name, it.id.value) },
    )
}
