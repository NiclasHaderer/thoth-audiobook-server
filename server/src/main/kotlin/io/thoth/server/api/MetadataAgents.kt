package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.metadata.MetadataAgents
import io.thoth.openapi.ktor.get
import org.koin.ktor.ext.inject

fun Routing.metadataAgentRouting() {
    val scanners by inject<MetadataAgents>()
    get<Api.MetadataAgents, List<MetadataAgentApiModel>> {
        scanners.map { MetadataAgentApiModel(it.name, it.supportedCountryCodes) }
    }
}
