package audible.models

import kotlinx.serialization.Serializable

interface AudibleAuthor : AudibleSearchAuthor {
    val image: String?
    val biography: String?
}

@Serializable
class AudibleAuthorImpl(
    override val image: String?,
    override val biography: String?,
    override val asin: String,
    override val name: String?,
    override val link: String,
) : AudibleAuthor
