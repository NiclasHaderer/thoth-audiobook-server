package io.thoth.metadata.libby.models

import io.thoth.metadata.libby.client.LIBBY_PROVIDER_NAME
import io.thoth.metadata.responses.MetadataAgentID

internal data class LibbyAgentId(
    override val itemID: String,
) : MetadataAgentID {
    override val provider = LIBBY_PROVIDER_NAME
}
