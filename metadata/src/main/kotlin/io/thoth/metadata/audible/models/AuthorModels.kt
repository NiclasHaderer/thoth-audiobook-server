package io.thoth.metadata.audible.models

import io.thoth.metadata.AuthorMetadata


class AudibleAuthorImpl(
    override val id: AudibleProviderWithIDMetadata,
    override val image: String?,
    override val biography: String?,
    override val name: String?,
    override val link: String,
) : AuthorMetadata
