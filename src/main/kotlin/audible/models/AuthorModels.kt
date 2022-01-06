package audible.models

import io.github.huiibuh.metadata.AuthorMetadata
import io.github.huiibuh.metadata.ProviderWithID
import kotlinx.serialization.Serializable

@Serializable
class AudibleAuthorImpl(
    override val image: String?,
    override val biography: String?,
    override val id: ProviderWithID,
    override val name: String?,
    override val link: String,
) : AuthorMetadata
