package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

interface ISeriesModel {
    val id: UUID
    val title: String
    val amount: Long
    val providerID: ProviderIDModel?
    val description: String?
    val author: NamedId
    val images: List<@Serializable(UUIDSerializer::class) UUID>
}

@Serializable
data class SeriesModel(
    @Serializable(UUIDSerializer::class) override val id: UUID,
    override val title: String,
    override val amount: Long,
    override val providerID: ProviderIDModel?,
    override val description: String?,
    override val author: NamedId,
    override val images: List<@Serializable(UUIDSerializer::class) UUID>,
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
    override val amount: Long,
    val narrators: List<String>,
    val yearRange: YearRange?,
    val position: Int,
    override val providerID: ProviderIDModel?,
    override val description: String?,
    val books: List<BookModel>,
    override val author: NamedId,
    override val images: List<@Serializable(UUIDSerializer::class) UUID>,
) : ISeriesModel {
    companion object {
        fun fromModel(series: SeriesModel, books: List<BookModel>, position: Int): SeriesModelWithBooks {
            val narrators = books.mapNotNull { it.narrator }.distinctBy { it }
            val years = books.mapNotNull { it.year }
            val startYear = years.minOrNull()
            val endYear = years.maxOrNull()

            var yearRange: YearRange? = null
            if (startYear != null && endYear != null) {
                yearRange = YearRange(start = startYear, end = endYear)
            }

            return SeriesModelWithBooks(
                id = series.id,
                title = series.title,
                amount = series.amount,
                yearRange = yearRange,
                position = position,
                narrators = narrators,
                providerID = series.providerID,
                description = series.description,
                books = books,
                author = series.author,
                images = series.images
            )
        }
    }
}
