package io.thoth.metadata.responses

import java.time.LocalDate

interface MetadataBook {
    val description: String?
    val id: MetadataProviderWithID
    val title: String?
    val narrator: String?
    val link: String?
    val author: MetadataSearchAuthor?
    val series: MetadataSearchSeries?
    val image: String?
    val date: LocalDate?
}

data class MetadataBookImpl(
    override val description: String?,
    override val id: MetadataProviderWithID,
    override val title: String?,
    override val narrator: String?,
    override val link: String?,
    override val author: MetadataSearchAuthor?,
    override val series: MetadataSearchSeries?,
    override val image: String?,
    override val date: LocalDate?,
) : MetadataBook

interface MetadataSearchBook {
    val id: MetadataProviderWithID
    val title: String?
    val link: String?
    val author: MetadataSearchAuthor?
    val narrator: String?
    val series: MetadataSearchSeries?
    val cover: String?
    val language: String?
    val releaseDate: LocalDate?
}

data class MetadataSearchBookImpl(
    override val id: MetadataProviderWithID,
    override val title: String?,
    override val link: String?,
    override val author: MetadataSearchAuthor?,
    override val narrator: String?,
    override val series: MetadataSearchSeries?,
    override val cover: String?,
    override val language: String?,
    override val releaseDate: LocalDate?,
) : MetadataSearchBook