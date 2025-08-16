package io.thoth.server.database.access

import io.thoth.models.BookModel
import io.thoth.models.NamedId
import io.thoth.models.TitledId
import io.thoth.server.database.tables.Book
import org.jetbrains.exposed.sql.SortOrder

fun Book.toModel(
    authorOrder: SortOrder = SortOrder.ASC,
    seriesOrder: SortOrder = SortOrder.ASC,
): BookModel =
    BookModel(
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
                .let { if (authorOrder == SortOrder.DESC) it.reversed() else it },
        series =
            series
                .sortedBy { it.title.lowercase() }
                .map { TitledId(it.id.value, it.title) }
                .let { if (seriesOrder == SortOrder.DESC) it.reversed() else it },
        genres = genres.map { NamedId(it.id.value, it.name) },
        library = NamedId(library.id.value, library.name),
    )
