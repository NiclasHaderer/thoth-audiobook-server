package io.thoth.auth.utils

import io.ktor.server.auth.Principal
import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingContext
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.util.UUID

interface ThothPrincipal : Principal {
    val userId: UUID
    val type: ThothJwtTypes
}

inline fun <reified TYPE : ThothPrincipal> RoutingContext.thothPrincipal(): TYPE =
    call.principal()
        ?: throw ErrorResponse.internalError(
            "Route requires user to be logged in, but authentication was not required. Please check your configuration.",
        )
