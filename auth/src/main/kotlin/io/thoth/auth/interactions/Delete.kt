package io.thoth.auth.interactions

import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothDeleteUserParams {
    val id: Any
}

fun RouteHandler.deleteUser(
    params: ThothDeleteUserParams,
    body: Unit,
) {
    val principal = thothPrincipal<ThothPrincipal>()
    val config = thothAuthConfig()

    if (principal.userId != params.id && !principal.isAdmin) {
        throw ErrorResponse.forbidden("Delete", "user")
    }

    val user = config.getUserById(params.id) ?: throw ErrorResponse.userError("User not found")
    config.deleteUser(user)
}
