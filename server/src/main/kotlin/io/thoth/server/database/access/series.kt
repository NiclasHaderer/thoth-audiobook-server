package io.thoth.server.database.access

import io.thoth.models.NamedId
import io.thoth.models.SeriesModel
import io.thoth.models.TitledId
import io.thoth.server.database.tables.Series
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.lowerCase

fun Series.toModel(authorOrder: SortOrder = SortOrder.ASC): SeriesModel {

    return SeriesModel(
        id = id.value,
        title = title,
        description = description,
        providerID = providerID,
        provider = provider,
        coverID = coverID?.value,
        primaryWorks = primaryWorks,
        totalBooks = totalBooks,
        authors =
            authors.orderBy(io.thoth.server.database.tables.TAuthors.name.lowerCase() to authorOrder).map {
                NamedId(it.id.value, it.name)
            },
        genres = genres.map { NamedId(it.id.value, it.name) },
        library = TitledId(library.id.value, library.name),
    )
}
