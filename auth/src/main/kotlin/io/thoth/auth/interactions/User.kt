package io.thoth.auth.interactions

import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserPermissions
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.auth.utils.wrap
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothDisplayUserParams<T : Any> {
    val id: T
}

fun <ID : Any, PERMISSIONS : ThothUserPermissions> RouteHandler.displayUser(
    params: ThothDisplayUserParams<ID>
): ThothUser<ID, PERMISSIONS> {
    val principal = thothPrincipal<ThothPrincipal<ID, PERMISSIONS>>()
    val config = thothAuthConfig<ID, PERMISSIONS>()

    if (principal.userId != params.id && !principal.permissions.isAdmin) {
        throw ErrorResponse.forbidden("View", "account")
    }

    val user = config.getUserById(params.id) ?: throw ErrorResponse.notFound("User", params.id)
    return user.wrap()
}

interface ThothCurrentUserParams

fun <ID : Any, PERMISSIONS : ThothUserPermissions> RouteHandler.currentUser(
    params: ThothCurrentUserParams
): ThothUser<ID, PERMISSIONS> {
    val principal = thothPrincipal<ThothPrincipal<ID, PERMISSIONS>>()
    val config = thothAuthConfig<ID, PERMISSIONS>()

    val user = config.getUserById(principal.userId) ?: throw ErrorResponse.notFound("User", principal.userId)
    return user.wrap()
}
