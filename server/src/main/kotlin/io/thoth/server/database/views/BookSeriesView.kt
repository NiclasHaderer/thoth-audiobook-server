package io.thoth.server.database.views

import io.thoth.server.common.exposed.View
import io.thoth.server.database.tables.SeriesBookTable
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.select

object BookSeriesView : View("BookSeries") {
    val book = javaUUID("book")
    val series = javaUUID("series")
    val index = float("index").nullable()

    override fun body() =
        SeriesBookTable
            .join(BookMetadataView, JoinType.INNER, SeriesBookTable.book, BookMetadataView.id)
            .select(SeriesBookTable.book, SeriesBookTable.series, SeriesBookTable.seriesIndex)
            .where { SeriesBookTable.addedBy eq BookMetadataView.seriesFrom }
}
