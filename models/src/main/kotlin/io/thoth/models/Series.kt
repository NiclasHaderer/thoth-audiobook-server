package io.thoth.models

import java.util.*

open class SeriesModel(
    val id: UUID,
    val authors: List<NamedId>,
    val title: String,
    val provider: String?,
    val providerID: String?,
    val totalBooks: Int?,
    val primaryWorks: Int?,
    val coverID: UUID?,
    val description: String?,
    val genres: List<NamedId>,
    val library: TitledId
)

data class YearRange(
    val start: Int,
    val end: Int,
)

class DetailedSeriesModel(
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
    titledId: TitledId,
    val yearRange: YearRange?,
    val narrators: List<String>,
    val books: List<BookModel>,
) :
    SeriesModel(
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
        library = titledId,
    ) {
    companion object {
        fun fromModel(series: SeriesModel, books: List<BookModel>): DetailedSeriesModel {
            val narrators = books.mapNotNull { it.narrator }.distinctBy { it }
            val years = books.mapNotNull { it.releaseDate }
            val startDate = years.minOrNull()
            val endDate = years.maxOrNull()

            var yearRange: YearRange? = null
            if (startDate != null && endDate != null) {
                yearRange = YearRange(start = startDate.year, end = endDate.year)
            }

            return DetailedSeriesModel(
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
                titledId = series.library,
            )
        }
    }
}
