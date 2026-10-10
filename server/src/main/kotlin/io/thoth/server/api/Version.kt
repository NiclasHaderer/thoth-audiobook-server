package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.ApiVersion
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.plugins.OpenAPIConfigurationKey

fun Routing.versionRouting() {
    get<Api.Version, ApiVersion> { ApiVersion(call.application.attributes[OpenAPIConfigurationKey].apiVersion) }
}
