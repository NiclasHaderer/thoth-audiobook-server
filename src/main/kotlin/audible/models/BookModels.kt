package audible.models

import kotlinx.serialization.Serializable

interface AudibleBook {
    val description: String?
    val asin: String
    val title: String?
    val link: String?
    val author: AudibleSearchAuthor?
    val series: AudibleSearchSeries?
    val image: String?
}

@Serializable
class AudibleBookImpl(
    override val description: String?,
    override val asin: String,
    override val title: String?,
    override val link: String?,
    override val author: AudibleSearchAuthorImpl?,
    override val series: AudibleSearchSeriesImpl?,
    override val image: String?,
) : AudibleBook
