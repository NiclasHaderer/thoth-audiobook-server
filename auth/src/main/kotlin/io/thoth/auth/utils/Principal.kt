package io.thoth.auth.utils

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.auth.models.ThothUserPermissions
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothPrincipal<ID : Any, PERMISSIONS : ThothUserPermissions> : Principal {
    val userId: ID
    val type: ThothJwtTypes
    val permissions: PERMISSIONS
}

inline fun <reified TYPE : ThothPrincipal<*, *>> RouteHandler.thothPrincipal(): TYPE {
    return call.principal()
        ?: throw ErrorResponse.internalError(
            "Route requires user to be logged in, but authentication was not required. Please check your configuration."
        )
}
