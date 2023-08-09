package io.thoth.auth.utils

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothPrincipal<T : Any> : Principal {
    val userId: T
    val isAdmin: Boolean
    val type: ThothJwtTypes
    val permissions: Map<String, Any>
}

fun <T : Any> RouteHandler.thothPrincipal(): ThothPrincipal<T> {
    return call.principal()
        ?: throw ErrorResponse.internalError(
            "Route requires user to be logged in, but authentication was not required. Please check your configuration.",
        )
}
