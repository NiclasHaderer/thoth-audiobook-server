package io.thoth.metadata.audible.models

import io.thoth.metadata.audible.client.AUDIBLE_PROVIDER_NAME
import io.thoth.metadata.responses.MetadataAgentID

data class AudibleAgentId(
    override val itemID: String,
) : MetadataAgentID {
    override val provider = AUDIBLE_PROVIDER_NAME
}
