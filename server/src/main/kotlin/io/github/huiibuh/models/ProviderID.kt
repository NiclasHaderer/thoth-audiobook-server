package io.github.huiibuh.models

import io.github.huiibuh.metadata.ProviderWithIDMetadata

class ProviderIDModel(
    override val provider: String,
    override val itemID: String,
) : ProviderWithIDMetadata
