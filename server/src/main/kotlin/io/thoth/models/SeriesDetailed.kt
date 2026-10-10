package io.thoth.models

import io.thoth.server.database.tables.SeriesField
import java.util.UUID

class SeriesDetailed(
    id: UUID,
    libraryId: UUID,
    authors: List<NamedId>,
    title: String,
    provider: String?,
    providerID: String?,
    totalBooks: Int?,
    primaryWorks: Int?,
    coverID: UUID?,
    description: String?,
    genres: List<String>,
    bookCoverIDs: List<UUID>,
    val yearRange: YearRange?,
    val narrators: List<String>,
    val books: List<Book>,
    val locked: List<SeriesField>,
) : Series(
        id = id,
        libraryId = libraryId,
        title = title,
        authors = authors,
        provider = provider,
        providerID = providerID,
        totalBooks = totalBooks,
        primaryWorks = primaryWorks,
        coverID = coverID,
        description = description,
        genres = genres,
        bookCoverIDs = bookCoverIDs,
    ) {
    companion object {
        fun fromModel(
            series: Series,
            books: List<Book>,
            locked: List<SeriesField>,
        ): SeriesDetailed {
            val years = books.mapNotNull { it.releaseDate }

            return SeriesDetailed(
                id = series.id,
                libraryId = series.libraryId,
                title = series.title,
                totalBooks = series.totalBooks,
                yearRange = years.minOrNull()?.let { YearRange(start = it.year, end = years.max().year) },
                narrators = books.flatMap { it.narrators }.distinct(),
                description = series.description,
                books = books,
                authors = series.authors,
                primaryWorks = series.primaryWorks,
                coverID = series.coverID,
                provider = series.provider,
                providerID = series.providerID,
                genres = series.genres,
                bookCoverIDs = series.bookCoverIDs,
                locked = locked,
            )
        }
    }
}
