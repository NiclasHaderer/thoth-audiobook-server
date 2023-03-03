package io.thoth.models

import io.thoth.common.serializion.kotlin.UUID_S
import java.util.*
import kotlinx.serialization.Serializable

interface ISeriesModel {
    val id: UUID
    val authors: List<NamedId>
    val title: String
    val provider: String?
    val providerID: String?
    val totalBooks: Int?
    val primaryWorks: Int?
    val coverID: UUID?
    val description: String?
}

@Serializable
data class SeriesModel(
    override val id: UUID_S,
    override val authors: List<NamedId>,
    override val title: String,
    override val provider: String?,
    override val providerID: String?,
    override val totalBooks: Int?,
    override val primaryWorks: Int?,
    override val coverID: UUID_S?,
    override val description: String?
) : ISeriesModel

@Serializable
data class YearRange(
    val start: Int,
    val end: Int,
)

@Serializable
data class DetailedSeriesModel(
    override val id: UUID_S,
    override val authors: List<NamedId>,
    override val title: String,
    override val provider: String?,
    override val providerID: String?,
    override val totalBooks: Int?,
    override val primaryWorks: Int?,
    override val coverID: UUID_S?,
    override val description: String?,
    val yearRange: YearRange?,
    val narrators: List<String>,
    val books: List<IBookModel>,
) : ISeriesModel {
    companion object {
        fun fromModel(series: ISeriesModel, books: List<IBookModel>): DetailedSeriesModel {
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
            )
        }
    }
}
