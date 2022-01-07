package io.github.huiibuh.models

import io.github.huiibuh.metadata.ProviderWithIDMetadata
import kotlinx.serialization.Serializable

@Serializable
class ProviderIDModel(
    override val provider: String,
    override val itemID: String,
): ProviderWithIDMetadata
