package io.thoth.openapi.ktor.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.dataconversion.DataConversion
import io.ktor.server.resources.Resources
import io.ktor.util.AttributeKey
import io.thoth.openapi.ktor.OpenApiRouteCollector
import io.thoth.openapi.ktor.SchemaHolder
import io.thoth.openapi.ktor.models.OpenAPIContext
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

class OpenAPIConfiguration(
    val schemaHolder: SchemaHolder,
    val routeCollector: OpenApiRouteCollector,
) : OpenAPIContext(schemaHolder.api) {
    val apiVersion: String
        get() =
            requireNotNull(schemaHolder.api.info?.version) {
                "The API version is missing. Set it via install(OpenAPIRouting) { info { version = \"...\" } }."
            }

    fun addRoutesToSpec() = routeCollector.forEach { schemaHolder.addRouteToApi(it) }
}

val OpenAPIConfigurationKey = AttributeKey<OpenAPIConfiguration>(name = "OpenAPIConfiguration")

fun Application.generateOpenApiSpec(savePath: Path) {
    val config = attributes[OpenAPIConfigurationKey]
    config.addRoutesToSpec()
    savePath.createParentDirectories().writeText(config.schemaHolder.yaml())
}

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
