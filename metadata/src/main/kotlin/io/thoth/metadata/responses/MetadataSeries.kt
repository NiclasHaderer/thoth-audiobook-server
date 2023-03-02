package io.thoth.metadata.responses

interface MetadataBookSeries {
  val id: MetadataProviderWithID
  val title: String?
  val link: String
  val index: Float?
}

data class MetadataBookSeriesImpl(
    override val id: MetadataProviderWithID,
    override val title: String?,
    override val link: String,
    override val index: Float?,
) : MetadataBookSeries

interface MetadataSeries {
  val id: MetadataProviderWithID
  val link: String
  val title: String?
  val author: String?
  val coverURL: String?
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
    override val coverURL: String?,
    override val description: String?,
    override val totalBooks: Int?,
    override val primaryWorks: Int?,
    override val books: List<MetadataSearchBook>?,
) : MetadataSeries
