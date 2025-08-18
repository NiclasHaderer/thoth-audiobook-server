package io.thoth.auth.interactions

import io.ktor.server.routing.RoutingContext
import io.thoth.auth.models.ThothUserWithPermissions
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothListUserParams

fun <PERMISSIONS : Any> RoutingContext.listUsers(
    params: ThothListUserParams,
): List<ThothUserWithPermissions<PERMISSIONS>> {
    val config = thothAuthConfig<PERMISSIONS, Any>()

    val principal = thothPrincipal<ThothPrincipal>()
    if (!config.isAdmin(principal)) {
        throw ErrorResponse.userError("User is not admin")
    }

    return config.listAllUsers().map { user ->
        val permissions = config.run { getUserPermissions(user) }
        ThothUserWithPermissions(id = user.id, username = user.username, permissions = permissions)
    }
}
