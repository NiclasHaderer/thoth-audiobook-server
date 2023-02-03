package io.thoth.metadata.responses

interface MetadataSearchSeries {
    val id: MetadataProviderWithID
    val name: String?
    val index: Float?
    val link: String
}

data class MetadataSearchSeriesImpl(
    override val id: MetadataProviderWithID,
    override val name: String?,
    override val index: Float?,
    override val link: String,
) : MetadataSearchSeries

interface MetadataSeries {
    val id: MetadataProviderWithID
    val link: String
    val name: String?
    val description: String?
    val amount: Int?
    val books: List<MetadataSearchBook>?
    val author: String?
    val image: String?
}

data class MetadataSeriesImpl(
    override val id: MetadataProviderWithID,
    override val link: String,
    override val name: String?,
    override val description: String?,
    override val amount: Int?,
    override val books: List<MetadataSearchBook>?,
    override val author: String?,
    override val image: String?,
) : MetadataSeries