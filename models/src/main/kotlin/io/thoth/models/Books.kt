package io.thoth.models

import io.thoth.common.serializion.kotlin.LocalDate_S
import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.*

interface IBookModel {
    val id: UUID
    val title: String
    val authors: List<NamedId>
    val series: List<TitledId>
    val provider: String?
    val providerID: String?
    val providerRating: Float?
    val releaseDate: LocalDate?
    val publisher: String?
    val language: String?
    val description: String?
    val narrator: String?
    val isbn: String?
    val coverID: UUID?
}

@Serializable
data class BookModel(
    override val id: UUID_S,
    override val authors: List<NamedId>,
    override val series: List<TitledId>,
    override val title: String,
    override val provider: String?,
    override val providerID: String?,
    override val providerRating: Float?,
    override val releaseDate: LocalDate_S?,
    override val publisher: String?,
    override val language: String?,
    override val description: String?,
    override val narrator: String?,
    override val isbn: String?,
    override val coverID: UUID_S?
) : IBookModel

@Serializable
data class BookModelWithTracks(
    override val id: UUID_S,
    override val authors: List<NamedId>,
    override val series: List<TitledId>,
    override val title: String,
    override val provider: String?,
    override val providerID: String?,
    override val providerRating: Float?,
    override val releaseDate: LocalDate_S?,
    override val publisher: String?,
    override val language: String?,
    override val description: String?,
    override val narrator: String?,
    override val isbn: String?,
    override val coverID: UUID_S?,
    val tracks: List<TrackModel>,
) : IBookModel {
    companion object {
        fun fromModel(
            book: IBookModel, tracks: List<TrackModel>
        ): BookModelWithTracks {

            val sortedTracks = if (tracks.any { it.trackNr == null }) {
                tracks.sortedBy { it.path }
            } else {
                tracks.sortedBy { it.trackNr }
            }

            return BookModelWithTracks(
                id = book.id,
                title = book.title,
                releaseDate = book.releaseDate,
                language = book.language,
                description = book.description,
                tracks = sortedTracks,
                authors = book.authors,
                narrator = book.narrator,
                series = book.series,
                coverID = book.coverID,
                isbn = book.isbn,
                provider = book.provider,
                providerID = book.providerID,
                providerRating = book.providerRating,
                publisher = book.publisher,
            )
        }
    }
}
