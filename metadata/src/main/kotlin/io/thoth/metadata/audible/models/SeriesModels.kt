package io.thoth.metadata.audible.models

import io.thoth.metadata.SeriesMetadata

class AudibleSeriesImpl(
    override val id: AudibleProviderWithIDMetadata,
    override val link: String,
    override val name: String?,
    override val description: String?,
    override val amount: Int?,
    override val books: List<AudibleSearchBookImpl>?,
    override val author: String?,
) : SeriesMetadata
