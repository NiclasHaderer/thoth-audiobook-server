package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class SeriesModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val name: String,
    val asin: String?,
    val description: String?,
    @Serializable(UUIDSerializer::class) val author: UUID,
)

@Serializable
data class SeriesModelWithBooks(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val name: String,
    val asin: String?,
    val description: String?,
    val books: List<BookModel>,
    @Serializable(UUIDSerializer::class) val author: UUID,
) {
    companion object {
        fun fromModel(series: SeriesModel, books: List<BookModel>) = SeriesModelWithBooks(
            id = series.id,
            name = series.name,
            asin = series.asin,
            description = series.description,
            books = books,
            author = series.author
        )
    }
}
