package io.thoth.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.pipeline.*
import io.thoth.openapi.ErrorResponse

object Guards {
    const val Normal = "user-jwt"
    const val Edit = "edit-user-jwt"
    const val Admin = "admin-user-jwt"
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipal(): ThothPrincipal {
    return thothPrincipalOrNull()
        ?: throw ErrorResponse.internalError("Could not get principal. Route has to be guarded with one of the Guards")
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipalOrNull(): ThothPrincipal? = call.principal()
