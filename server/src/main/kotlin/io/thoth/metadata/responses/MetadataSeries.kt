package io.thoth.metadata.responses

interface MetadataBookSeries {
    val id: MetadataAgentID
    val title: String?
    val link: String
    val index: Float?
}

data class MetadataBookSeriesImpl(
    override val id: MetadataAgentID,
    override val title: String?,
    override val link: String,
    override val index: Float?,
) : MetadataBookSeries

interface MetadataSeries {
    val id: MetadataAgentID
    val link: String
    val title: String?
    val authors: List<String>?
    val coverURL: String?
    val description: String?
    val totalBooks: Int?
    val primaryWorks: Int?
    val books: List<MetadataSearchBook>?
}

data class MetadataSeriesImpl(
    override val id: MetadataAgentID,
    override val title: String?,
    override val authors: List<String>?,
    override val link: String,
    override val coverURL: String?,
    override val description: String?,
    override val totalBooks: Int?,
    override val primaryWorks: Int?,
    override val books: List<MetadataSearchBook>?,
) : MetadataSeries
