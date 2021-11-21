package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*


@Serializable
data class BookModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val title: String,
    val language: String?,
    val description: String?,
    val asin: String?,
    @Serializable(UUIDSerializer::class) val author: UUID,
    @Serializable(UUIDSerializer::class) val narrator: UUID?,
    @Serializable(UUIDSerializer::class) val series: UUID?,
    val seriesIndex: Int?,
    @Serializable(UUIDSerializer::class) val cover: UUID?,
)

@Serializable
data class BookWithTracks(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val title: String,
    val language: String?,
    val description: String?,
    val asin: String?,
    val tracks: List<TrackModel>,
    @Serializable(UUIDSerializer::class) val author: UUID,
    @Serializable(UUIDSerializer::class) val narrator: UUID?,
    @Serializable(UUIDSerializer::class) val series: UUID?,
    val seriesIndex: Int?,
    @Serializable(UUIDSerializer::class) val cover: UUID?,
) {
    companion object {
        fun fromBookModel(book: BookModel, tracks: List<TrackModel>) = BookWithTracks(
            id = book.id,
            title = book.title,
            language = book.language,
            description = book.description,
            asin = book.asin,
            tracks = tracks,
            author = book.author,
            narrator = book.narrator,
            series = book.series,
            seriesIndex = book.seriesIndex,
            cover = book.cover
        )
    }
}
