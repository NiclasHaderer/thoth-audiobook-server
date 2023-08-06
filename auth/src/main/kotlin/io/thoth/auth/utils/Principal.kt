package io.thoth.auth.utils

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothPrincipal : Principal {
    val userId: Any
    val isAdmin: Boolean
}

inline fun <reified T : ThothPrincipal> RouteHandler.thothPrincipal(): T {
    return call.principal()
        ?: throw ErrorResponse.internalError(
            "Route requires user to be logged in, but authentication was not required. Please check your configuration."
        )
}
