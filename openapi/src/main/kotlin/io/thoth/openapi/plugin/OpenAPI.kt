package io.thoth.openapi.plugin

import io.ktor.server.application.*
import io.ktor.server.plugins.dataconversion.*
import io.ktor.server.resources.*
import io.swagger.v3.oas.models.OpenAPI
import io.thoth.openapi.SchemaHolder
import io.thoth.openapi.models.generalOpenApiContext

val OpenAPIRouting =
    createApplicationPlugin(
        "OpenAPIRouting",
        createConfiguration = {
            val api = OpenAPI()
            SchemaHolder.set(api)
            generalOpenApiContext(api)
        }
    ) {

        // Ensure that the dataconversion plugin is installed
        application.plugin(DataConversion)
        application.plugin(Resources)
        application.environment.monitor.subscribe(ApplicationStarted) { SchemaHolder.lock() }
    }
