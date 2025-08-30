package io.thoth.server.database.access

import io.thoth.models.NamedId
import io.thoth.models.SeriesModel
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.SeriesEntity
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.lowerCase

fun SeriesEntity.toModel(authorOrder: SortOrder = SortOrder.ASC): SeriesModel =
    SeriesModel(
        id = id.value,
        title = title,
        description = description,
        providerID = providerID,
        provider = provider,
        coverID = coverID?.value,
        primaryWorks = primaryWorks,
        totalBooks = totalBooks,
        authors = authors.orderBy(AuthorTable.name.lowerCase() to authorOrder).map { NamedId(it.id.value, it.name) },
        genres = genres.map { NamedId(it.id.value, it.name) },
        library = NamedId(library.id.value, library.name),
    )
