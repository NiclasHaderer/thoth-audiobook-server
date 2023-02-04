package io.thoth.metadata.responses

interface MetadataSearchSeries {
    val id: MetadataProviderWithID
    val title: String?
    val author: String?
    val link: String
    val cover: String?
}

data class MetadataSearchSeriesImpl(
    override val id: MetadataProviderWithID,
    override val title: String?,
    override val author: String?,
    override val link: String,
    override val cover: String?,
) : MetadataSearchSeries

interface MetadataSeries : MetadataSearchSeries {
    val description: String?
    val totalBooks: Int?
    val primaryWorks: Int?
    val books: List<MetadataSearchBook>?
}

data class MetadataSeriesImpl(
    override val id: MetadataProviderWithID,
    override val title: String?,
    override val author: String?,
    override val link: String,
    override val cover: String?,
    override val description: String?,
    override val totalBooks: Int?,
    override val primaryWorks: Int?,
    override val books: List<MetadataSearchBook>?,
) : MetadataSeries