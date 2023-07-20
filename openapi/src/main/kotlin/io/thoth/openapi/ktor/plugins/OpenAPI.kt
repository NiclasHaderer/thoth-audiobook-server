package io.thoth.openapi.ktor.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.dataconversion.*
import io.ktor.server.resources.*
import io.thoth.openapi.ktor.SchemaHolder
import io.thoth.openapi.ktor.models.OpenAPIContext

val OpenAPIRouting =
    createApplicationPlugin(
        "OpenAPIRouting",
        createConfiguration = { OpenAPIContext.generate(SchemaHolder.api) },
    ) {

        // Ensure that the dataconversion plugins is installed
        application.plugin(DataConversion)
        application.plugin(Resources)
        application.plugin(ContentNegotiation)
    }
