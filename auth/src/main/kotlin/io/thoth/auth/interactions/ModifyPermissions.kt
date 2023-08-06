package io.thoth.auth.interactions

import io.thoth.auth.ThothAuthConfig
import io.thoth.auth.models.ModifyPermissions
import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserImpl
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

    val user = ThothAuthConfig.getUserById(params.id) ?: throw ErrorResponse.notFound("User", params.id)

    return ThothAuthConfig.updateUserPermissions(user, body.permissions, body.isAdmin).let { ThothUserImpl.wrap(it) }
}
