package io.thoth.metadata.audible.models

import io.thoth.metadata.audible.client.AUDIBLE_PROVIDER_NAME
import io.thoth.metadata.responses.MetadataProviderWithID

data class AudibleProviderWithIDMetadata(override val itemID: String) : MetadataProviderWithID {
  override val provider = AUDIBLE_PROVIDER_NAME
}
