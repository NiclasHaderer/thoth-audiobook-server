package io.thoth.openapi.plugin

import io.ktor.server.application.*

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

    private var _openAPISchemaPath = "/docs/openapi"
    var schemaPath
        get() = (_openAPISchemaPath + if (schemaType == OpenAPISchemaType.YAML) ".yaml" else ".json")
        set(value) {
            this._openAPISchemaPath = value.trim('/')
        }
}

val OpenAPIWebUI =
    createApplicationPlugin("OpenAPIWebUI", createConfiguration = { WebUiConfig() }) {
        // Ensure the OpenAPIRouting plugin is installed
        application.plugin(OpenAPIRouting)

        val webUiServer = WebUiServer(pluginConfig)

        this.onCall { call -> webUiServer.interceptCall(call) }
    }
