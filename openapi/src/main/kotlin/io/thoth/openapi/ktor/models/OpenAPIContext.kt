package io.thoth.openapi.ktor.models

import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.servers.Server

open class OpenAPIContext internal constructor(
    private val api: OpenAPI,
) {
    companion object {
        fun generate(api: OpenAPI? = null): OpenAPIContext = OpenAPIContext(api ?: OpenAPI())
    }

    fun info(configure: InfoContext.() -> Unit) {
        if (api.info == null) {
            api.info = Info()
        }
        InfoContext(api.info).apply(configure)
    }

    fun externalDocs(configure: ExternalDocumentation.() -> Unit) {
        if (api.externalDocs == null) {
            api.externalDocs = ExternalDocumentation()
        }

        api.externalDocs.apply(configure)
    }

    fun addServer(configure: Server.() -> Unit) {
        if (api.servers == null) {
            api.servers = mutableListOf()
        }
        api.servers.add(Server().apply(configure))
    }

    fun addSecurity(configure: SecurityRequirement.() -> Unit) {
        if (api.security == null) {
            api.security = mutableListOf()
        }

        api.security.add(SecurityRequirement().apply(configure))
    }

    fun components(configure: ComponentsContext.() -> Unit) {
        if (api.components == null) {
            api.components =
                io.swagger.v3.oas.models
                    .Components()
        }
        ComponentsContext(api.components).apply(configure)
    }
}
