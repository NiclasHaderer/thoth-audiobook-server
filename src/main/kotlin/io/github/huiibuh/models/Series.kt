package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class SeriesModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val title: String,
    val amount: Long,
    val asin: String?,
    val description: String?,
    val author: NamedId,
)

@Serializable
data class YearRange(
    val start: Int,
    val end: Int
)

@Serializable
data class SeriesModelWithBooks(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val title: String,
    val amount: Long,
    val narrators: List<String>,
    val yearRange: YearRange?,
    val position: Int,
    val asin: String?,
    val description: String?,
    val books: List<BookModel>,
    val author: NamedId,
) {
    companion object {
        fun fromModel(series: SeriesModel, books: List<BookModel>, position: Int): SeriesModelWithBooks {
            val narrators = books.mapNotNull { it.narrator }.distinctBy { it }
            val years = books.mapNotNull { it.year }
            val startYear = years.minOrNull()
            val endYear = years.maxOrNull()

            var yearRange: YearRange? = null
            if(startYear != null && endYear != null){
                yearRange = YearRange(start = startYear, end = endYear)
            }

            return SeriesModelWithBooks(
                id = series.id,
                title = series.title,
                amount = series.amount,
                yearRange = yearRange,
                position = position,
                narrators = narrators,
                asin = series.asin,
                description = series.description,
                books = books,
                author = series.author
            )
        }
    }
}
