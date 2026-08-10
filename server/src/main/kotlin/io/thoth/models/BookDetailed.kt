package io.thoth.models

import java.time.LocalDate
import java.util.UUID

class BookDetailed(
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
    genres: List<String>,
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
    ) {
    companion object {
        fun fromModel(
            book: Book,
            tracks: List<Track>,
        ) = BookDetailed(
            id = book.id,
            title = book.title,
            releaseDate = book.releaseDate,
            language = book.language,
            description = book.description,
            tracks = tracks,
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
