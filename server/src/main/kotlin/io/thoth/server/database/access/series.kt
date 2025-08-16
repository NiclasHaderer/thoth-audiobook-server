package io.thoth.server.database.access

import io.thoth.models.NamedId
import io.thoth.models.SeriesModel
import io.thoth.server.database.tables.Series
import io.thoth.server.database.tables.TAuthors
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.lowerCase

fun Series.toModel(authorOrder: SortOrder = SortOrder.ASC): SeriesModel =
    SeriesModel(
        id = id.value,
        title = title,
        description = description,
        providerID = providerID,
        provider = provider,
        coverID = coverID?.value,
        primaryWorks = primaryWorks,
        totalBooks = totalBooks,
        authors = authors.orderBy(TAuthors.name.lowerCase() to authorOrder).map { NamedId(it.id.value, it.name) },
        genres = genres.map { NamedId(it.id.value, it.name) },
        library = NamedId(library.id.value, library.name),
    )
