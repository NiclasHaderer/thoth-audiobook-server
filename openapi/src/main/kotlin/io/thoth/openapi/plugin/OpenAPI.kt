package io.thoth.openapi.plugin

import io.ktor.server.application.*
import io.ktor.server.plugins.dataconversion.*
import io.ktor.server.resources.*
import io.thoth.openapi.SchemaHolder
import io.thoth.openapi.models.generalOpenApiContext

val OpenAPIRouting =
    createApplicationPlugin(
        "OpenAPIRouting",
        createConfiguration = { generalOpenApiContext(SchemaHolder.api) },
    ) {

        // Ensure that the dataconversion plugin is installed
        application.plugin(DataConversion)
        application.plugin(Resources)
        application.environment.monitor.subscribe(ApplicationStarted) {
            val t = SchemaHolder.json()
            println(t)
        }
    }
