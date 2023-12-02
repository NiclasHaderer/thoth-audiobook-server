package io.thoth.openapi.ktor.plugins

import io.ktor.server.application.*
import io.thoth.openapi.ktor.OpenApiRouteCollector
import io.thoth.openapi.ktor.SchemaHolder

enum class OpenAPISchemaType(val extension: String) {
    JSON("json"),
    YAML("yaml")
}

class WebUiConfig internal constructor() {
    val webUiVersion = "4.15.5"
    private lateinit var _docsPath: String
    private lateinit var _schemaPath: String

    var schemaType = OpenAPISchemaType.JSON

    var docsPath
        get() = _docsPath
        set(value) {
            this._docsPath = value.trimEnd('/').prependIfMissing('/')
        }

    var schemaPath
        get() = "$_schemaPath.${schemaType.extension}"
        set(value) {
            this._schemaPath = value.trimEnd('/').prependIfMissing('/')
        }

    init {
        schemaPath = "/docs/openapi"
        docsPath = "/docs"
    }
}

val OpenAPIWebUI = createApplicationPlugin("OpenAPIWebUI", createConfiguration = { WebUiConfig() }) {
    application.environment.monitor.subscribe(ApplicationStarted) {

        // Get the plugin configuration
        val pluginConfig = application.attributes.get(OpenAPIConfigurationKey)

        try {
            pluginConfig.routeCollector.forEach { pluginConfig.schemaHolder.addRouteToApi(it) }
        } catch (e: Exception) {
            it.log.error("Error while adding routes to API. OpenApi document is not complete!", e)
        }
    }

    val webUiServer = WebUiServer(pluginConfig)
    this.onCall { call -> webUiServer.interceptCall(call) }
}

private fun String.prependIfMissing(char: Char): String {
    return if (isNotEmpty() && this[0] != char) {
        char + this
    } else {
        this
    }
}
