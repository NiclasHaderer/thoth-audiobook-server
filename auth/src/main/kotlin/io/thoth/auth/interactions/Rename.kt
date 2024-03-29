package io.thoth.auth.interactions

import io.thoth.auth.models.ThothRenameUser
import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserPermissions
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.auth.utils.wrap
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothRenameUserParams<T : Any> {
    val id: T
}

fun <ID : Any, PERMISSIONS : ThothUserPermissions> RouteHandler.renameUser(
    params: ThothRenameUserParams<ID>,
    renamedUser: ThothRenameUser
): ThothUser<ID, PERMISSIONS> {
    val principal = thothPrincipal<ThothPrincipal<ID, PERMISSIONS>>()

    if (principal.userId != params.id && !principal.permissions.isAdmin) {
        throw ErrorResponse.forbidden("Rename", "account")
    }

    val config = thothAuthConfig<ID, PERMISSIONS>()

    config.usernameMeetsRequirements(renamedUser.username).also { (meetsRequirement, message) ->
        if (!meetsRequirement) {
            throw ErrorResponse.userError(message!!)
        }
    }

    config.getUserByUsername(renamedUser.username)?.let { throw ErrorResponse.userError("Username already exists") }

    var user = config.getUserById(params.id) ?: throw ErrorResponse.notFound("User", params.id)
    user = config.renameUser(user, renamedUser.username)
    return user.wrap()
}
