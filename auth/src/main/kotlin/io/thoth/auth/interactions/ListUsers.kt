package io.thoth.auth.interactions

import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserImpl
import io.thoth.auth.models.ThothUserPermissions
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothListUserParams

fun <ID : Any, PERMISSIONS : ThothUserPermissions> RouteHandler.listUsers(
    params: ThothListUserParams,
): List<ThothUser<ID, PERMISSIONS>> {
    val principal = thothPrincipal<ThothPrincipal<ID, PERMISSIONS>>()
    if (!principal.permissions.isAdmin) {
        throw ErrorResponse.userError("User is not admin")
    }

    val config = thothAuthConfig<ID, PERMISSIONS>()
    return config.listAllUsers().map { ThothUserImpl.wrap(it) }
}
