package io.github.huiibuh.metadata.audible.models

import io.github.huiibuh.metadata.BookMetadata
import kotlinx.serialization.Serializable


@Serializable
class AudibleBookImpl(
    override val description: String?,
    override val id: AudibleProviderWithIDMetadata,
    override val title: String?,
    override val link: String?,
    override val author: AudibleSearchAuthorImpl?,
    override val series: AudibleSearchSeriesImpl?,
    override val image: String?,
    override val narrator: String?,
) : BookMetadata
