package io.github.huiibuh.metadata.audible.models

import io.github.huiibuh.metadata.SeriesMetadata
import kotlinx.serialization.Serializable

@Serializable
class AudibleSeriesImpl(
    override val id: AudibleProviderWithIDMetadata,
    override val link: String,
    override val name: String?,
    override val description: String?,
    override val amount: Int?,
    override val books: List<AudibleSearchBookImpl>?,
) : SeriesMetadata
