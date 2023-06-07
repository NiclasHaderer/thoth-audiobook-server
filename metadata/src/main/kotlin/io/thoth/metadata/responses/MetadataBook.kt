package io.thoth.metadata.responses

import java.time.LocalDate

interface MetadataSearchBook {
    val id: MetadataProviderWithID
    val title: String?
    val link: String?
    val authors: List<MetadataSearchAuthor>?
    val series: List<MetadataBookSeries>
    val language: String?
    val releaseDate: LocalDate?
    val coverURL: String?
    val narrator: String?
}

data class MetadataSearchBookImpl(
    override val id: MetadataProviderWithID,
    override val title: String?,
    override val link: String?,
    override val authors: List<MetadataSearchAuthor>?,
    override val series: List<MetadataBookSeries>,
    override val language: String?,
    override val releaseDate: LocalDate?,
    override val coverURL: String?,
    override val narrator: String?,
) : MetadataSearchBook

interface MetadataBook : MetadataSearchBook {
    val description: String?
    val providerRating: Float?
    val publisher: String?
    val isbn: String?
}

data class MetadataBookImpl(
    override val id: MetadataProviderWithID,
    override val title: String?,
    override val link: String?,
    override val authors: List<MetadataSearchAuthor>?,
    override val series: List<MetadataBookSeries>,
    override val releaseDate: LocalDate?,
    override val coverURL: String?,
    override val description: String?,
    override val narrator: String?,
    override val providerRating: Float?,
    override val publisher: String?,
    override val language: String?,
    override val isbn: String?,
) : MetadataBook
