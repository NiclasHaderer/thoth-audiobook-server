package io.thoth.models

import io.thoth.common.serializion.kotlin.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

interface ISeriesModel {
    val id: UUID
    val title: String
    val provider: String?
    val providerID: String?
    val totalBooks: Int?
    val primaryWorks: Int?
    val cover: UUID?
    val description: String?
}

@Serializable
data class SeriesModel(
    @Serializable(UUIDSerializer::class) override val id: UUID,
    override val title: String,
    override val provider: String?,
    override val providerID: String?,
    override val totalBooks: Int?,
    override val primaryWorks: Int?,
    @Serializable(UUIDSerializer::class) override val cover: UUID?,
    override val description: String?
) : ISeriesModel

@Serializable
data class YearRange(
    val start: Int,
    val end: Int,
)

@Serializable
data class SeriesModelWithBooks(
    @Serializable(UUIDSerializer::class) override val id: UUID,
    override val title: String,
    override val provider: String?,
    override val providerID: String?,
    override val totalBooks: Int?,
    override val primaryWorks: Int?,
    @Serializable(UUIDSerializer::class) override val cover: UUID?,
    override val description: String?,
    val yearRange: YearRange?,
    val narrators: List<String>,
    val books: List<IBookModel>,
    val authors: List<IAuthorModel>,
) : ISeriesModel {
    companion object {
        fun fromModel(
            series: ISeriesModel, books: List<IBookModel>, authors: List<IAuthorModel>
        ): SeriesModelWithBooks {
            val narrators = books.mapNotNull { it.narrator }.distinctBy { it }
            val years = books.mapNotNull { it.releaseDate }
            val startDate = years.minOrNull()
            val endDate = years.maxOrNull()

            var yearRange: YearRange? = null
            if (startDate != null && endDate != null) {
                yearRange = YearRange(start = startDate.year, end = endDate.year)
            }

            return SeriesModelWithBooks(
                id = series.id,
                title = series.title,
                totalBooks = series.totalBooks,
                yearRange = yearRange,
                narrators = narrators,
                description = series.description,
                books = books,
                authors = authors,
                primaryWorks = series.primaryWorks,
                cover = series.cover,
                provider = series.provider,
                providerID = series.providerID,
            )
        }
    }
}
