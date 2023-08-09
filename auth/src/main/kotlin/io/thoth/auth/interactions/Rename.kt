package io.thoth.auth.interactions

import io.thoth.auth.models.ThothRenameUser
import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserImpl
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothRenameUserParams<T : Any> {
    val id: T
}

fun <T : Any> RouteHandler.renameUser(params: ThothRenameUserParams<T>, renamedUser: ThothRenameUser): ThothUser<T> {
    val principal = thothPrincipal<ThothPrincipal<T>>()

    if (principal.userId != params.id && !principal.isAdmin) {
        throw ErrorResponse.forbidden("Rename", "account")
    }

    val config = thothAuthConfig()

    config.usernameMeetsRequirements(renamedUser.username).also { (meetsRequirement, message) ->
        if (!meetsRequirement) {
            throw ErrorResponse.userError(message!!)
        }
    }

    config.getUserByUsername(renamedUser.username)?.let { throw ErrorResponse.userError("Username already exists") }

    var user = config.getUserById(params.id) ?: throw ErrorResponse.notFound("User", params.id)
    user = config.renameUser(user, renamedUser.username)
    return ThothUserImpl.wrap(user) as ThothUser<T>
}
