package io.thoth.models.datastructures

import io.thoth.metadata.ProviderWithIDMetadata

class ProviderIDModel(
    override val provider: String,
    override val itemID: String,
) : ProviderWithIDMetadata
