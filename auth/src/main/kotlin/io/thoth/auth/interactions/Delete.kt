package io.thoth.auth.interactions

import io.thoth.auth.models.ThothUserPermissions
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothDeleteUserParams<T : Any> {
    val id: T
}

fun <ID : Any, PERMISSIONS : ThothUserPermissions> RouteHandler.deleteUser(
    params: ThothDeleteUserParams<ID>,
    body: Unit,
) {
    val principal = thothPrincipal<ThothPrincipal<ID, PERMISSIONS>>()
    val config = thothAuthConfig()

    if (principal.userId != params.id && !principal.permissions.isAdmin) {
        throw ErrorResponse.forbidden("Delete", "user")
    }

    val user = config.getUserById(params.id) ?: throw ErrorResponse.userError("User not found")
    config.deleteUser(user)
}
