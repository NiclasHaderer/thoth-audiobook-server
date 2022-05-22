package io.thoth.openapi.plugin

import io.ktor.server.application.*

enum class OpenAPISchemaType {
    JSON,
    YAML
}

class WebUiConfig internal constructor() {
    var schemaType = OpenAPISchemaType.JSON
    var webUiVersion = "4.4.1-1"
    private var _openAPIUiPath = "/docs"
    var webUiPath
        get() = normalizeURL(_openAPIUiPath)
        set(value) {
            this._openAPIUiPath = value
        }

    private var _openAPISchemaPath = "/docs/openapi"
    var schemaPath
        get() = normalizeURL(_openAPISchemaPath + if (schemaType == OpenAPISchemaType.YAML) ".yaml" else ".json")
        set(value) {
            this._openAPISchemaPath = value
        }

    fun normalizeURL(url: String) = "/${url.trim('/')}/"
}

val OpenAPIWebUI = createApplicationPlugin("OpenAPIWebUI", createConfiguration = { WebUiConfig() }) {
    // Ensure the OpenAPIRouting plugin is installed
    application.plugin(OpenAPIRouting)

    val webUiServer = WebUiServer(pluginConfig)

    this.onCall { call -> webUiServer.interceptCall(call) }
}


