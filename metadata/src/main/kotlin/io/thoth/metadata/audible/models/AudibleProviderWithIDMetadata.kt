package io.thoth.metadata.audible.models

import io.thoth.metadata.ProviderWithIDMetadata
import io.thoth.metadata.audible.client.AUDIBLE_PROVIDER_NAME

class AudibleProviderWithIDMetadata(override val itemID: String) : ProviderWithIDMetadata {
    override val provider = AUDIBLE_PROVIDER_NAME
}
