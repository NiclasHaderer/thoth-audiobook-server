package io.thoth.auth.interactions

import io.thoth.auth.models.ModifyPermissions
import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserImpl
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothModifyPermissionsParams {
    val id: Any
}

fun RouteHandler.modifyUserPermissions(params: ThothModifyPermissionsParams, body: ModifyPermissions): ThothUser {
    val principal = thothPrincipal<ThothPrincipal>()

    if (!principal.isAdmin) {
        throw ErrorResponse.forbidden("Modify", "permissions")
    }
    val config = thothAuthConfig()

    val user = config.getUserById(params.id) ?: throw ErrorResponse.notFound("User", params.id)
    return config.updateUserPermissions(user, body.permissions, body.isAdmin).let { ThothUserImpl.wrap(it) }
}
