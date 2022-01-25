package io.github.huiibuh.models

import io.github.huiibuh.serializers.LocalDateSerializer
import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.*

interface IBookModel {
    val id: UUID
    val title: String
    val year: Int?
    val language: String?
    val description: String?
    val providerID: ProviderIDModel?
    val author: NamedId
    val narrator: String?
    val series: TitledId?
    val seriesIndex: Float?
    val cover: UUID?
    val updateTime: LocalDateTime
}

@Serializable
data class BookModel(
    @Serializable(UUIDSerializer::class) override val id: UUID,
    override val title: String,
    override val year: Int?,
    override val language: String?,
    override val description: String?,
    override val providerID: ProviderIDModel?,
    override val author: NamedId,
    override val narrator: String?,
    override val series: TitledId?,
    override val seriesIndex: Float?,
    @Serializable(UUIDSerializer::class) override val cover: UUID?,
    @Serializable(LocalDateSerializer::class) override val updateTime: LocalDateTime,
) : IBookModel

@Serializable
data class BookModelWithTracks(
    @Serializable(UUIDSerializer::class) override val id: UUID,
    override val title: String,
    override val year: Int?,
    override val language: String?,
    override val description: String?,
    override val providerID: ProviderIDModel?,
    override val author: NamedId,
    override val narrator: String?,
    override val series: TitledId?,
    override val seriesIndex: Float?,
    @Serializable(UUIDSerializer::class) override val cover: UUID?,
    val position: Int,
    val tracks: List<TrackModel>,
    @Serializable(LocalDateSerializer::class) override val updateTime: LocalDateTime,
) : IBookModel {
    companion object {
        fun fromModel(book: BookModel, tracks: List<TrackModel>, position: Int): BookModelWithTracks {

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
                providerID = book.providerID,
                position = position,
                tracks = sortedTracks,
                author = book.author,
                narrator = book.narrator,
                series = book.series,
                updateTime = book.updateTime,
                seriesIndex = book.seriesIndex,
                cover = book.cover
            )
        }
    }
}
