package io.thoth.models

import java.time.LocalDate
import java.util.UUID

class DetailedBook(
    id: UUID,
    authors: List<NamedId>,
    series: List<TitledId>,
    title: String,
    provider: String?,
    providerID: String?,
    providerRating: Float?,
    releaseDate: LocalDate?,
    publisher: String?,
    language: String?,
    description: String?,
    narrator: String?,
    isbn: String?,
    coverID: UUID?,
    genres: List<NamedId>,
    library: NamedId,
    val tracks: List<Track>,
) : Book(
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
        library = library,
    ) {
    companion object {
        fun fromModel(
            book: Book,
            tracks: List<Track>,
        ): DetailedBook {
            val sortedTracks =
                if (tracks.any { it.trackNr == null }) {
                    tracks.sortedBy { it.path }
                } else {
                    tracks.sortedBy { it.trackNr }
                }

            return DetailedBook(
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
                library = book.library,
            )
        }
    }
}
