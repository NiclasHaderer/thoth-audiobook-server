package io.github.huiibuh.metadata.impl.audible.models

import io.github.huiibuh.metadata.BookMetadata
import io.github.huiibuh.metadata.ProviderWithIDMetadata
import kotlinx.serialization.Serializable


@Serializable
class AudibleBookImpl(
    override val description: String?,
    override val id: ProviderWithIDMetadata,
    override val title: String?,
    override val link: String?,
    override val author: AudibleSearchAuthorImpl?,
    override val series: AudibleSearchSeriesImpl?,
    override val image: String?,
) : BookMetadata
