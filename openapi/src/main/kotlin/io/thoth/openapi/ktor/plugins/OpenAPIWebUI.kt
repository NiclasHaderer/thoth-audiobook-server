package io.thoth.openapi.ktor.plugins

import io.ktor.server.application.*
import io.thoth.openapi.ktor.OpenApiRouteCollector
import io.thoth.openapi.ktor.SchemaHolder

enum class OpenAPISchemaType {
    JSON,
    YAML
}

class WebUiConfig internal constructor() {
    var schemaType = OpenAPISchemaType.JSON
    val webUiVersion = "4.15.5"
    private var _openAPIUiPath = "/docs"
    var webUiPath
        get() = _openAPIUiPath
        set(value) {
            this._openAPIUiPath = value.trim('/')
        }

    private var _openAPISchemaPath = "/docs/ktor"
    var schemaPath
        get() = (_openAPISchemaPath + if (schemaType == OpenAPISchemaType.YAML) ".yaml" else ".json")
        set(value) {
            this._openAPISchemaPath = value.trim('/')
        }
}

val OpenAPIWebUI =
    createApplicationPlugin("OpenAPIWebUI", createConfiguration = { WebUiConfig() }) {
        application.environment.monitor.subscribe(ApplicationStarted) {
            OpenApiRouteCollector.forEach { SchemaHolder.addRouteToApi(it) }
        }

        val webUiServer = WebUiServer(pluginConfig)
        this.onCall { call -> webUiServer.interceptCall(call) }
    }
