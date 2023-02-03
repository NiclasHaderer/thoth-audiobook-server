package io.thoth.models

import java.time.LocalDate
import java.util.*

interface IBookModel {
    val id: UUID
    val title: String
    val provider: String?
    val providerID: String?
    val providerRating: Float?
    val published: LocalDate?
    val publisher: String?
    val language: String?
    val description: String?
    val narrator: String?
    val isbn: String?
    val cover: UUID?
}

class BookModel(
    override val id: UUID,
    override val title: String,
    override val provider: String?,
    override val providerID: String?,
    override val providerRating: Float?,
    override val published: LocalDate?,
    override val publisher: String?,
    override val language: String?,
    override val description: String?,
    override val narrator: String?,
    override val isbn: String?,
    override val cover: UUID?,
) : IBookModel

class BookModelWithTracks(
    override val id: UUID,
    override val title: String,
    override val provider: String?,
    override val providerID: String?,
    override val providerRating: Float?,
    override val published: LocalDate?,
    override val publisher: String?,
    override val language: String?,
    override val description: String?,
    override val narrator: String?,
    override val isbn: String?,
    override val cover: UUID?,
    val tracks: List<TrackModel>,
    val authors: List<IAuthorModel>,
    val series: List<ISeriesModel>,
) : IBookModel {
    companion object {
        fun fromModel(
            book: IBookModel, tracks: List<TrackModel>, authors: List<IAuthorModel>, series: List<ISeriesModel>
        ): BookModelWithTracks {

            val sortedTracks = if (tracks.any { it.trackNr == null }) {
                tracks.sortedBy { it.path }
            } else {
                tracks.sortedBy { it.trackNr }
            }

            return BookModelWithTracks(
                id = book.id,
                title = book.title,
                published = book.published,
                language = book.language,
                description = book.description,
                tracks = sortedTracks,
                authors = authors,
                narrator = book.narrator,
                series = series,
                cover = book.cover,
                isbn = book.isbn,
                provider = book.provider,
                providerID = book.providerID,
                providerRating = book.providerRating,
                publisher = book.publisher,
            )
        }
    }
}
