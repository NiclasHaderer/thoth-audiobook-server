package io.thoth.models

import io.thoth.metadata.responses.MetadataLanguage
import java.time.LocalDate
import java.util.UUID

open class Book(
    val id: UUID,
    val libraryId: UUID,
    val authors: List<NamedId>,
    val series: List<TitledId>,
    val title: String,
    val provider: String?,
    val providerID: String?,
    val providerRating: Float?,
    val releaseDate: LocalDate?,
    val publisher: String?,
    val language: MetadataLanguage?,
    val description: String?,
    val narrators: List<String>,
    val isbn: String?,
    val coverID: UUID?,
    val genres: List<String>,
    val durationMs: Long,
    val positionMs: Long,
    val status: PlayStatus,
)
