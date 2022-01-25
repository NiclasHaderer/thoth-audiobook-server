package io.github.huiibuh.metadata.audible.models

import io.github.huiibuh.metadata.AuthorMetadata
import kotlinx.serialization.Serializable

@Serializable
class AudibleAuthorImpl(
    override val image: String?,
    override val biography: String?,
    override val id: AudibleProviderWithIDMetadata,
    override val name: String?,
    override val link: String,
) : AuthorMetadata
