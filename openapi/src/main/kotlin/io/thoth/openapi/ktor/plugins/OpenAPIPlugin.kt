package io.thoth.openapi.ktor.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.dataconversion.*
import io.ktor.server.resources.*
import io.ktor.util.*
import io.thoth.openapi.ktor.OpenApiRouteCollector
import io.thoth.openapi.ktor.SchemaHolder
import io.thoth.openapi.ktor.models.OpenAPIContext

class OpenAPIConfiguration(val schemaHolder: SchemaHolder, val routeCollector: OpenApiRouteCollector) :
    OpenAPIContext(schemaHolder.api)

val OpenAPIConfigurationKey = AttributeKey<OpenAPIConfiguration>(name = "OpenAPIConfiguration")

val OpenAPIRouting =
    createApplicationPlugin(
        "OpenAPIRouting",
        createConfiguration = {
            OpenAPIConfiguration(schemaHolder = SchemaHolder(), routeCollector = OpenApiRouteCollector())
        },
    ) {
        application.attributes.put(OpenAPIConfigurationKey, pluginConfig)

        // Ensure that the plugins are installed
        application.plugin(DataConversion)
        application.plugin(Resources)
        application.plugin(ContentNegotiation)
    }
