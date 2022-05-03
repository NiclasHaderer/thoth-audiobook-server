package io.thoth.models

import io.thoth.metadata.ProviderWithIDMetadata

class ProviderIDModel(
    override val provider: String,
    override val itemID: String,
) : ProviderWithIDMetadata
