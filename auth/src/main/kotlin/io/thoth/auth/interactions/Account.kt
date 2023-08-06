package io.thoth.auth.interactions

import io.thoth.auth.ThothAuthConfig
import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserImpl
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothAccountParams {
    val id: Any
}

fun RouteHandler.account(params: ThothAccountParams): ThothUser {
    val principal = thothPrincipal<ThothPrincipal>()

    if (principal.userId != params.id && !principal.isAdmin) {
        throw ErrorResponse.forbidden("View", "account")
    }

    val user = ThothAuthConfig.getUserById(params.id) ?: throw ErrorResponse.notFound("User", params.id)
    return ThothUserImpl.wrap(user)
}
