package io.thoth.metadata.audible.models

import io.thoth.metadata.BookMetadata


class AudibleBookImpl(
    override val id: AudibleProviderWithIDMetadata,
    override val description: String?,
    override val title: String?,
    override val link: String?,
    override val author: AudibleSearchAuthorImpl?,
    override val series: AudibleSearchSeriesImpl?,
    override val image: String?,
    override val narrator: String?,
    override val year: Int?,
) : BookMetadata
