package io.thoth.metadata.responses

import java.time.LocalDate

interface MetadataSearchBook {
    val id: MetadataProviderWithID
    val title: String?
    val link: String?
    val author: MetadataSearchAuthor?
    val series: MetadataSearchSeries?
    val releaseDate: LocalDate?
    val cover: String?
}

data class MetadataSearchBookImpl(
    override val id: MetadataProviderWithID,
    override val title: String?,
    override val link: String?,
    override val author: MetadataSearchAuthor?,
    override val series: MetadataSearchSeries?,
    override val releaseDate: LocalDate?,
    override val cover: String?,
) : MetadataSearchBook

interface MetadataBook : MetadataSearchBook {
    val description: String?
    val narrator: String?
    val providerRating: Float?
    val publisher: String?
    val language: String?
    val isbn: String?
}

data class MetadataBookImpl(
    override val id: MetadataProviderWithID,
    override val title: String?,
    override val link: String?,
    override val author: MetadataSearchAuthor?,
    override val series: MetadataSearchSeries?,
    override val releaseDate: LocalDate?,
    override val cover: String?,
    override val description: String?,
    override val narrator: String?,
    override val providerRating: Float?,
    override val publisher: String?,
    override val language: String?,
    override val isbn: String?,
) : MetadataBook

