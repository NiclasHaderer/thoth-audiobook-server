package io.github.huiibuh.metadata.audible.models

import io.github.huiibuh.metadata.AuthorMetadata

class AudibleAuthorImpl(
    override val image: String?,
    override val biography: String?,
    override val id: AudibleProviderWithIDMetadata,
    override val name: String?,
    override val link: String,
) : AuthorMetadata
