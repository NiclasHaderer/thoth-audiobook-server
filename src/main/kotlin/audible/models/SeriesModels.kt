package audible.models

import kotlinx.serialization.Serializable

interface AudibleSeries {
    val asin: String
    val link: String
    val name: String?
    val description: String?
    val amount: Int?
    val books: List<AudibleSearchResult>
}

@Serializable
class AudibleSeriesImpl(
    override val asin: String,
    override val link: String,
    override val name: String?,
    override val description: String?,
    override val amount: Int?,
    override val books: List<AudibleSearchResultImpl>,
) : AudibleSeries
