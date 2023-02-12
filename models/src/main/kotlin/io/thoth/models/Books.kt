package io.thoth.models

import io.thoth.common.serializion.kotlin.LocalDateSerializer
import io.thoth.common.serializion.kotlin.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.*

interface IBookModel {
    val id: UUID
    val title: String
    val provider: String?
    val providerID: String?
    val providerRating: Float?
    val releaseDate: LocalDate?
    val publisher: String?
    val language: String?
    val description: String?
    val narrator: String?
    val isbn: String?
    val cover: UUID?
}

@Serializable
data class BookModel(
    @Serializable(UUIDSerializer::class) override val id: UUID,
    override val title: String,
    override val provider: String?,
    override val providerID: String?,
    override val providerRating: Float?,
    @Serializable(LocalDateSerializer::class) override val releaseDate: LocalDate?,
    override val publisher: String?,
    override val language: String?,
    override val description: String?,
    override val narrator: String?,
    override val isbn: String?,
    @Serializable(UUIDSerializer::class) override val cover: UUID?,
) : IBookModel

@Serializable
data class BookModelWithTracks(
    @Serializable(UUIDSerializer::class) override val id: UUID,
    override val title: String,
    override val provider: String?,
    override val providerID: String?,
    override val providerRating: Float?,
    @Serializable(LocalDateSerializer::class) override val releaseDate: LocalDate?,
    override val publisher: String?,
    override val language: String?,
    override val description: String?,
    override val narrator: String?,
    override val isbn: String?,
    @Serializable(UUIDSerializer::class) override val cover: UUID?,
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
                releaseDate = book.releaseDate,
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
