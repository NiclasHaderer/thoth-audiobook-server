package io.thoth.auth.interactions

import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserWithPermissions
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.auth.utils.wrap
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.util.*

interface ThothDisplayUserParams {
    val id: UUID
}

fun RouteHandler.displayUser(params: ThothDisplayUserParams): ThothUser {
    val principal = thothPrincipal<ThothPrincipal>()
    val config = thothAuthConfig<Any>()

    if (principal.userId != params.id && !config.isAdmin(principal)) {
        throw ErrorResponse.forbidden("View", "account")
    }

    val user = config.getUserById(params.id) ?: throw ErrorResponse.notFound("User", params.id)
    return user.wrap()
}

interface ThothCurrentUserParams

fun <PERMISSIONS : Any> RouteHandler.currentUser(
    params: ThothCurrentUserParams,
): ThothUserWithPermissions<PERMISSIONS> {
    val principal = thothPrincipal<ThothPrincipal>()
    val config = thothAuthConfig<PERMISSIONS>()

    val user = config.getUserById(principal.userId) ?: throw ErrorResponse.notFound("User", principal.userId)
    return user.let {
        val permissions = config.run { getUserPermissions(user) }
        ThothUserWithPermissions(id = user.id, username = user.username, permissions = permissions)
    }
}
