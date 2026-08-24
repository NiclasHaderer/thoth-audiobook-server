package io.thoth.server.api

import io.thoth.metadata.responses.MetadataRegion

data class MetadataAgentApiModel(
    val name: String,
    val supportedRegions: List<MetadataRegion>,
)
