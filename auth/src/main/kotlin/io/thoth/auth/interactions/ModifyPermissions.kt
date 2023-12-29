package io.thoth.auth.interactions

import io.thoth.auth.models.ThothModifyPermissions
import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserPermissions
import io.thoth.auth.models.ThothUserImpl
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.auth.utils.wrap
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothModifyPermissionsParams<T> {
    val id: T
}

fun <ID : Any, PERMISSIONS : ThothUserPermissions> RouteHandler.modifyUserPermissions(
    params: ThothModifyPermissionsParams<ID>,
    body: ThothModifyPermissions
): ThothUser<ID, PERMISSIONS> {
    val principal = thothPrincipal<ThothPrincipal<ID, PERMISSIONS>>()

    if (!principal.permissions.isAdmin) {
        throw ErrorResponse.forbidden("Modify", "permissions")
    }
    val config = thothAuthConfig<ID, PERMISSIONS>()

    val user = config.getUserById(params.id) ?: throw ErrorResponse.notFound("User", params.id)
    return config.updateUserPermissions(user, body.permissions).let { it.wrap() }
}
