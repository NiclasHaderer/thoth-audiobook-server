package io.thoth.openapi.openapi.models

import io.swagger.v3.oas.models.Components

class ComponentsContext(private val components: Components) {

    fun securitySchemes(configure: SecuritySchemesContext.() -> Unit) {
        if (components.securitySchemes == null) {
            components.securitySchemes = mutableMapOf()
        }
        SecuritySchemesContext(components.securitySchemes).apply(configure)
    }
}
