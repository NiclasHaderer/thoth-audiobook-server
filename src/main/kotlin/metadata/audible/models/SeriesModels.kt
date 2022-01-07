package metadata.audible.models

import io.github.huiibuh.metadata.ProviderWithIDMetadata
import io.github.huiibuh.metadata.SeriesMetadata
import kotlinx.serialization.Serializable

@Serializable
class AudibleSeriesImpl(
    override val id: ProviderWithIDMetadata,
    override val link: String,
    override val name: String?,
    override val description: String?,
    override val amount: Int?,
    override val books: List<AudibleSearchResultImpl>,
) : SeriesMetadata
