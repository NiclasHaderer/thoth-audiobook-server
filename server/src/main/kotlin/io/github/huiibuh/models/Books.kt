package io.github.huiibuh.models

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

class BookModel(
    override val id: UUID,
    override val title: String,
    override val year: Int?,
    override val language: String?,
    override val description: String?,
    override val providerID: ProviderIDModel?,
    override val author: NamedId,
    override val narrator: String?,
    override val series: TitledId?,
    override val seriesIndex: Float?,
    override val cover: UUID?,
    override val updateTime: LocalDateTime,
) : IBookModel

class BookModelWithTracks(
    override val id: UUID,
    override val title: String,
    override val year: Int?,
    override val language: String?,
    override val description: String?,
    override val providerID: ProviderIDModel?,
    override val author: NamedId,
    override val narrator: String?,
    override val series: TitledId?,
    override val seriesIndex: Float?,
    override val cover: UUID?,
    val position: Int,
    val tracks: List<TrackModel>,
    override val updateTime: LocalDateTime,
) : IBookModel {
    companion object {
        fun fromModel(book: IBookModel, tracks: List<TrackModel>, position: Int): BookModelWithTracks {

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
