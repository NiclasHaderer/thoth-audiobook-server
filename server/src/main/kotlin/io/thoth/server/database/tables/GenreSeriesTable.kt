package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable

object GenreSeriesTable : CompositeIdTable("GenreSeries") {
    val genre = reference("genre", GenresTable, onDelete = ReferenceOption.CASCADE)
    val series = reference("Series", SeriesTable, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(genre, series)

    init {
        addIdColumn(series)
        addIdColumn(genre)
    }
}
