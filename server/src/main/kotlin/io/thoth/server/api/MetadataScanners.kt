package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.metadata.MetadataAgents
import io.thoth.openapi.ktor.get
import org.koin.ktor.ext.inject

fun Routing.metadataScannerRouting() {
    val scanners by inject<MetadataAgents>()
    get<Api.MetadataScanners, List<MetadataAgentApiModel>> {
        scanners.map { MetadataAgentApiModel(it.name, it.supportedCountryCodes) }
    }
}
