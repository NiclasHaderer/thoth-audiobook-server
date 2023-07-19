package io.thoth.openapi.ktor.models

import io.swagger.v3.oas.models.security.SecurityScheme

class SecuritySchemesContext(private val securitySchemes: MutableMap<String, SecurityScheme>) {
    fun apiKey(name: String, configure: SecurityScheme.() -> Unit) {
        securitySchemes[name] =
            SecurityScheme().apply {
                type = SecurityScheme.Type.APIKEY
                configure()
            }
    }

    fun http(name: String, configure: SecurityScheme.() -> Unit) {
        securitySchemes[name] =
            SecurityScheme().apply {
                type = SecurityScheme.Type.HTTP
                configure()
            }
    }

    fun openIdConnect(name: String, configure: SecurityScheme.() -> Unit) {
        securitySchemes[name] =
            SecurityScheme().apply {
                type = SecurityScheme.Type.OPENIDCONNECT
                configure()
            }
    }

    fun oauth2(name: String, configure: SecurityScheme.() -> Unit) {
        securitySchemes[name] =
            SecurityScheme().apply {
                type = SecurityScheme.Type.OAUTH2
                configure()
            }
    }
}
