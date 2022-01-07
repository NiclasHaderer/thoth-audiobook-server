package metadata.audible.models

import io.github.huiibuh.metadata.AuthorMetadata
import io.github.huiibuh.metadata.ProviderWithIDMetadata
import kotlinx.serialization.Serializable

@Serializable
class AudibleAuthorImpl(
    override val image: String?,
    override val biography: String?,
    override val id: ProviderWithIDMetadata,
    override val name: String?,
    override val link: String,
) : AuthorMetadata
