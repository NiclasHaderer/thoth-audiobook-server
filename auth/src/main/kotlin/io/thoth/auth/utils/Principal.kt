package io.thoth.auth.utils

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.util.*

interface ThothPrincipal : Principal {
    val userId: UUID
    val type: ThothJwtTypes
}

inline fun <reified TYPE : ThothPrincipal> RouteHandler.thothPrincipal(): TYPE {
    return call.principal()
        ?: throw ErrorResponse.internalError(
            "Route requires user to be logged in, but authentication was not required. Please check your configuration."
        )
}
