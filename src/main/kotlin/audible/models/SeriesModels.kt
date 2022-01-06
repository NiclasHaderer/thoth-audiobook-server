package audible.models

import io.github.huiibuh.metadata.ProviderWithID
import io.github.huiibuh.metadata.SeriesMetadata
import kotlinx.serialization.Serializable

@Serializable
class AudibleSeriesImpl(
    override val id: ProviderWithID,
    override val link: String,
    override val name: String?,
    override val description: String?,
    override val amount: Int?,
    override val books: List<AudibleSearchResultImpl>,
) : SeriesMetadata
