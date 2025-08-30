package io.thoth.models

import java.util.UUID

class DetailedSeries(
    id: UUID,
    authors: List<NamedId>,
    title: String,
    provider: String?,
    providerID: String?,
    totalBooks: Int?,
    primaryWorks: Int?,
    coverID: UUID?,
    description: String?,
    genres: List<NamedId>,
    library: NamedId,
    val yearRange: YearRange?,
    val narrators: List<String>,
    val books: List<Book>,
) : Series(
        id = id,
        title = title,
        authors = authors,
        provider = provider,
        providerID = providerID,
        totalBooks = totalBooks,
        primaryWorks = primaryWorks,
        coverID = coverID,
        description = description,
        genres = genres,
        library = library,
    ) {
    companion object {
        fun fromModel(
            series: Series,
            books: List<Book>,
        ): DetailedSeries {
            val narrators = books.mapNotNull { it.narrator }.distinctBy { it }
            val years = books.mapNotNull { it.releaseDate }
            val startDate = years.minOrNull()
            val endDate = years.maxOrNull()

            var yearRange: YearRange? = null
            if (startDate != null && endDate != null) {
                yearRange = YearRange(start = startDate.year, end = endDate.year)
            }

            return DetailedSeries(
                id = series.id,
                title = series.title,
                totalBooks = series.totalBooks,
                yearRange = yearRange,
                narrators = narrators,
                description = series.description,
                books = books,
                authors = series.authors,
                primaryWorks = series.primaryWorks,
                coverID = series.coverID,
                provider = series.provider,
                providerID = series.providerID,
                genres = series.genres,
                library = series.library,
            )
        }
    }
}
