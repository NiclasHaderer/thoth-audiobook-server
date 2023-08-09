package io.thoth.auth.interactions

import io.thoth.auth.models.ThothModifyPermissions
import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserImpl
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothModifyPermissionsParams<T> {
    val id: T
}

fun <T : Any> RouteHandler.modifyUserPermissions(
    params: ThothModifyPermissionsParams<T>,
    body: ThothModifyPermissions
): ThothUser<T> {
    val principal = thothPrincipal<ThothPrincipal<T>>()

    if (!principal.isAdmin) {
        throw ErrorResponse.forbidden("Modify", "permissions")
    }
    val config = thothAuthConfig()

    val user = config.getUserById(params.id) ?: throw ErrorResponse.notFound("User", params.id)
    return config.updateUserPermissions(user, body.permissions, body.isAdmin).let { ThothUserImpl.wrap(it) }
        as ThothUser<T>
}
