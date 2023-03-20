package io.thoth.database.access

import io.thoth.database.tables.Book
import io.thoth.models.BookModel
import io.thoth.models.NamedId
import io.thoth.models.TitledId
import org.jetbrains.exposed.sql.SortOrder

fun Book.toModel(order: SortOrder = SortOrder.ASC): BookModel {
    return BookModel(
        id = id.value,
        title = title,
        description = description,
        providerID = providerID,
        provider = provider,
        providerRating = providerRating,
        coverID = coverID?.value,
        releaseDate = releaseDate,
        narrator = narrator,
        isbn = isbn,
        language = language,
        publisher = publisher,
        authors =
            authors
                .sortedBy { it.name.lowercase() }
                .map { NamedId(it.id.value, it.name) }
                .let { if (order == SortOrder.DESC) it.reversed() else it },
        series =
            series
                .sortedBy { it.title.lowercase() }
                .map { TitledId(it.id.value, it.title) }
                .let { if (order == SortOrder.DESC) it.reversed() else it },
        genres = genres.map { NamedId(it.id.value, it.name) },
    )
}
