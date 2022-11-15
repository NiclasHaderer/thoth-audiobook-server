package io.thoth.models

import java.time.LocalDateTime
import java.util.*

interface ISeriesModel {
    val id: UUID
    val title: String
    val amount: Long
    val description: String?
    val author: NamedId
    val images: List<UUID>
    val updateTime: LocalDateTime
}

class SeriesModel(
    override val id: UUID,
    override val title: String,
    override val amount: Long,
    override val description: String?,
    override val author: NamedId,
    override val images: List<UUID>,
    override val updateTime: LocalDateTime,
) : ISeriesModel

class YearRange(
    val start: Int,
    val end: Int,
)

class SeriesModelWithBooks(
    override val id: UUID,
    override val title: String,
    override val amount: Long,
    val narrators: List<String>,
    val yearRange: YearRange?,
    val position: Int,
    override val description: String?,
    val books: List<IBookModel>,
    override val author: NamedId,
    override val images: List<UUID>,
    override val updateTime: LocalDateTime,

    ) : ISeriesModel {
    companion object {
        fun fromModel(series: ISeriesModel, books: List<IBookModel>, position: Int): SeriesModelWithBooks {
            val narrators = books.mapNotNull { it.narrator }.distinctBy { it }
            val years = books.mapNotNull { it.date }
            val startDate = years.minOrNull()
            val endDate = years.maxOrNull()

            var yearRange: YearRange? = null
            if (startDate != null && endDate != null) {
                yearRange = YearRange(start = startDate.year, end = endDate.year)
            }

            return SeriesModelWithBooks(
                id = series.id,
                title = series.title,
                amount = series.amount,
                yearRange = yearRange,
                position = position,
                narrators = narrators,
                description = series.description,
                books = books,
                author = series.author,
                images = series.images,
                updateTime = series.updateTime
            )
        }
    }
}
