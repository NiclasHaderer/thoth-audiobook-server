package io.thoth.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.pipeline.*
import io.thoth.openapi.serverError

object Guards {
    const val Normal = "user-jwt"
    const val Edit = "edit-user-jwt"
    const val Admin = "admin-user-jwt"
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipal(): ThothPrincipal {
    return thothPrincipalOrNull()
        ?: serverError(HttpStatusCode.InternalServerError, "Call has to be surrounded with a jwt login")
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipalOrNull(): ThothPrincipal? = call.principal()
