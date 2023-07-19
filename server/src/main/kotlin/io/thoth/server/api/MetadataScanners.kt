package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.metadata.MetadataProviders
import io.thoth.openapi.openapi.get
import org.koin.ktor.ext.inject

fun Routing.metadataScannerRouting() {
    val scanners by inject<MetadataProviders>()
    get<Api.MetadataScanners, List<MetadataAgentApiModel>> {
        scanners.map { MetadataAgentApiModel(it.uniqueName, it.supportedCountryCodes) }
    }
}
