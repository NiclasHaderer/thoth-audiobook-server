package io.thoth.auth.interactions

import io.ktor.server.routing.RoutingContext
import io.thoth.auth.models.ThothModifyPermissions
import io.thoth.auth.models.ThothUser
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.auth.utils.wrap
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.util.UUID

interface ThothModifyPermissionsParams {
    val id: UUID
}

fun <UPDATE_PERMISSIONS> RoutingContext.modifyUserPermissions(
    params: ThothModifyPermissionsParams,
    body: ThothModifyPermissions<UPDATE_PERMISSIONS>,
): ThothUser {
    val principal = thothPrincipal<ThothPrincipal>()
    val config = thothAuthConfig<Any, UPDATE_PERMISSIONS>()

    if (!config.isAdmin(principal)) {
        throw ErrorResponse.forbidden("Modify", "permissions")
    }

    val user = config.getUserById(params.id) ?: throw ErrorResponse.notFound("User", params.id)
    return config.updateUserPermissions(user, body.permissions).wrap()
}
