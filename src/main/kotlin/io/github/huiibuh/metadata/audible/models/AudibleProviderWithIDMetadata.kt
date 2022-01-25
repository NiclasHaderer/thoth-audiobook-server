package io.github.huiibuh.metadata.audible.models

import io.github.huiibuh.metadata.ProviderWithIDMetadata
import io.github.huiibuh.metadata.audible.client.AUDIBLE_PROVIDER_NAME
import kotlinx.serialization.Serializable

@Serializable
class AudibleProviderWithIDMetadata(override val itemID: String) : ProviderWithIDMetadata {
    override val provider = AUDIBLE_PROVIDER_NAME
}
