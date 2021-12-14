package io.github.huiibuh.models

import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*


@Serializable
data class BookModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val title: String,
    val year: Int?,
    val language: String?,
    val description: String?,
    val asin: String?,
    val author: NamedId,
    val narrator: String?,
    val series: TitledId?,
    val seriesIndex: Float?,
    @Serializable(UUIDSerializer::class) val cover: UUID?,
)

@Serializable
data class BookModelWithTracks(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val title: String,
    val year: Int?,
    val language: String?,
    val description: String?,
    val asin: String?,
    val position: Int,
    val tracks: List<TrackModel>,
    val author: NamedId,
    val narrator: String?,
    val series: TitledId?,
    val seriesIndex: Float?,
    @Serializable(UUIDSerializer::class) val cover: UUID?,
) {
    companion object {
        fun fromModel(book: BookModel, tracks: List<Track>, position: Int): BookModelWithTracks {

            val sortedTracks = if (tracks.any { it.trackNr == null }) {
                tracks.sortedBy { it.path }
            } else {
                tracks.sortedBy { it.trackNr }
            }

            return BookModelWithTracks(
                id = book.id,
                title = book.title,
                year = book.year,
                language = book.language,
                description = book.description,
                asin = book.asin,
                position = position,
                tracks = sortedTracks.map { it.toModel() },
                author = book.author,
                narrator = book.narrator,
                series = book.series,
                seriesIndex = book.seriesIndex,
                cover = book.cover
            )
        }
    }
}
