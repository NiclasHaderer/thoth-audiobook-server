package io.thoth.metadata.responses

import io.thoth.models.ChapterMark
import java.time.LocalDate

interface MetadataSearchBook {
    val id: MetadataAgentID
    val title: String?
    val link: String?
    val authors: List<MetadataSearchAuthor>?
    val series: List<MetadataBookSeries>
    val language: MetadataLanguage?
    val releaseDate: LocalDate?
    val coverURL: String?
    val narrators: List<String>
}

data class MetadataSearchBookImpl(
    override val id: MetadataAgentID,
    override val title: String?,
    override val link: String?,
    override val authors: List<MetadataSearchAuthor>?,
    override val series: List<MetadataBookSeries>,
    override val language: MetadataLanguage?,
    override val releaseDate: LocalDate?,
    override val coverURL: String?,
    override val narrators: List<String>,
) : MetadataSearchBook

interface MetadataBook : MetadataSearchBook {
    val description: String?
    val providerRating: Float?
    val publisher: String?
    val isbn: String?
}

// The timings belong to the provider's edition of the book. Its runtime is what tells whether they fit the files at
// hand, an abridged or differently mastered edition would put every chapter in the wrong place.
data class MetadataChapters(
    val runtimeMs: Long,
    val chapters: List<ChapterMark>,
)

data class MetadataBookImpl(
    override val id: MetadataAgentID,
    override val title: String?,
    override val link: String?,
    override val authors: List<MetadataSearchAuthor>?,
    override val series: List<MetadataBookSeries>,
    override val releaseDate: LocalDate?,
    override val coverURL: String?,
    override val description: String?,
    override val narrators: List<String>,
    override val providerRating: Float?,
    override val publisher: String?,
    override val language: MetadataLanguage?,
    override val isbn: String?,
) : MetadataBook
