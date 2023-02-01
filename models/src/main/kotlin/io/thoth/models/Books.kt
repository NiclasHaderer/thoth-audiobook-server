package io.thoth.models

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface IBookModel {
    val id: UUID
    val title: String
    val date: LocalDate?
    val language: String?
    val description: String?
    val authors: List<NamedId>
    val narrator: String?
    val series: List<TitledId>
    val seriesIndex: Float?
    val cover: UUID?
    val updateTime: LocalDateTime
}

class BookModel(
    override val id: UUID,
    override val title: String,
    override val date: LocalDate?,
    override val language: String?,
    override val description: String?,
    override val authors: List<NamedId>,
    override val narrator: String?,
    override val series: List<TitledId>,
    override val seriesIndex: Float?,
    override val cover: UUID?,
    override val updateTime: LocalDateTime,
) : IBookModel

class BookModelWithTracks(
    override val id: UUID,
    override val title: String,
    override val date: LocalDate?,
    override val language: String?,
    override val description: String?,
    override val authors: List<NamedId>,
    override val narrator: String?,
    override val series: List<TitledId>,
    override val seriesIndex: Float?,
    override val cover: UUID?,
    val position: Long,
    val tracks: List<TrackModel>,
    override val updateTime: LocalDateTime,
) : IBookModel {
    companion object {
        fun fromModel(book: IBookModel, tracks: List<TrackModel>, position: Long): BookModelWithTracks {

            val sortedTracks = if (tracks.any { it.trackNr == null }) {
                tracks.sortedBy { it.path }
            } else {
                tracks.sortedBy { it.trackNr }
            }

            return BookModelWithTracks(
                id = book.id,
                title = book.title,
                date = book.date,
                language = book.language,
                description = book.description,
                position = position,
                tracks = sortedTracks,
                authors = book.authors,
                narrator = book.narrator,
                series = book.series,
                updateTime = book.updateTime,
                seriesIndex = book.seriesIndex,
                cover = book.cover
            )
        }
    }
}
