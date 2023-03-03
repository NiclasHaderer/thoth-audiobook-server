package io.thoth.openapi.models

import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.servers.Server

class OpenAPIContext internal constructor(private val api: OpenAPI) {
    fun info(configure: InfoContext.() -> Unit) {
        InfoContext(api).apply(configure)
    }

    fun externalDocs(configure: ExternalDocumentation.() -> Unit) {
        api.externalDocs = ExternalDocumentation().apply(configure)
    }

    fun server(configure: Server.() -> Unit) {
        if (api.servers == null) {
            api.servers = mutableListOf()
        }
        api.servers.add(Server().apply(configure))
    }

    fun security(configure: SecurityRequirement.() -> Unit) {
        if (api.security == null) {
            api.security = mutableListOf()
        }

        api.security.add(SecurityRequirement().apply(configure))
    }

    override fun toString() = toJson()

    fun toJson(): String {
        return Json.mapper().writeValueAsString(api)
    }

    fun toYml(): String {
        return Yaml.mapper().writeValueAsString(api)
    }

    fun load(newAPI: OpenAPI) {
        api.info = newAPI.info
        api.externalDocs = newAPI.externalDocs
        api.servers = newAPI.servers
        api.security = newAPI.security
    }
}
