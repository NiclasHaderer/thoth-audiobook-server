package io.thoth.models

import io.thoth.metadata.responses.MetadataLanguage
import java.time.LocalDate
import java.util.UUID

class BookDetailed(
    id: UUID,
    libraryId: UUID,
    authors: List<NamedId>,
    series: List<TitledId>,
    title: String,
    provider: String?,
    providerID: String?,
    providerRating: Float?,
    releaseDate: LocalDate?,
    publisher: String?,
    language: MetadataLanguage?,
    description: String?,
    narrators: List<String>,
    isbn: String?,
    coverID: UUID?,
    genres: List<String>,
    durationMs: Long,
    positionMs: Long,
    status: PlayStatus,
    val tracks: List<Track>,
) : Book(
        id = id,
        libraryId = libraryId,
        title = title,
        releaseDate = releaseDate,
        language = language,
        description = description,
        authors = authors,
        narrators = narrators,
        series = series,
        coverID = coverID,
        isbn = isbn,
        provider = provider,
        providerID = providerID,
        providerRating = providerRating,
        publisher = publisher,
        genres = genres,
        durationMs = durationMs,
        positionMs = positionMs,
        status = status,
    ) {
    companion object {
        fun fromModel(
            book: Book,
            tracks: List<Track>,
        ) = BookDetailed(
            id = book.id,
            libraryId = book.libraryId,
            title = book.title,
            releaseDate = book.releaseDate,
            language = book.language,
            description = book.description,
            tracks = tracks,
            authors = book.authors,
            narrators = book.narrators,
            series = book.series,
            coverID = book.coverID,
            isbn = book.isbn,
            provider = book.provider,
            providerID = book.providerID,
            providerRating = book.providerRating,
            publisher = book.publisher,
            genres = book.genres,
            durationMs = book.durationMs,
            positionMs = book.positionMs,
            status = book.status,
        )
    }
}
