package io.thoth.models

import io.thoth.common.serializion.kotlin.LocalDate_S
import io.thoth.common.serializion.kotlin.UUID_S

open class BookModel(
    val id: UUID_S,
    val authors: List<NamedId>,
    val series: List<TitledId>,
    val title: String,
    val provider: String?,
    val providerID: String?,
    val providerRating: Float?,
    val releaseDate: LocalDate_S?,
    val publisher: String?,
    val language: String?,
    val description: String?,
    val narrator: String?,
    val isbn: String?,
    val coverID: UUID_S?,
    val genres: List<NamedId>,
)

class DetailedBookModel(
    id: UUID_S,
    authors: List<NamedId>,
    series: List<TitledId>,
    title: String,
    provider: String?,
    providerID: String?,
    providerRating: Float?,
    releaseDate: LocalDate_S?,
    publisher: String?,
    language: String?,
    description: String?,
    narrator: String?,
    isbn: String?,
    coverID: UUID_S?,
    genres: List<NamedId>,
    val tracks: List<TrackModel>,
) :
    BookModel(
        id = id,
        title = title,
        releaseDate = releaseDate,
        language = language,
        description = description,
        authors = authors,
        narrator = narrator,
        series = series,
        coverID = coverID,
        isbn = isbn,
        provider = provider,
        providerID = providerID,
        providerRating = providerRating,
        publisher = publisher,
        genres = genres,
    ) {
    companion object {
        fun fromModel(book: BookModel, tracks: List<TrackModel>): DetailedBookModel {

            val sortedTracks =
                if (tracks.any { it.trackNr == null }) {
                    tracks.sortedBy { it.path }
                } else {
                    tracks.sortedBy { it.trackNr }
                }

            return DetailedBookModel(
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
                genres = book.genres,
            )
        }
    }
}
