package io.thoth.auth.interactions

import io.thoth.auth.ThothAuthConfig
import io.thoth.auth.models.ThothRenameUser
import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserImpl
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothRenameUserAccountParams {
    val id: Any
}

fun RouteHandler.renameUserAccount(params: ThothRenameUserAccountParams, renamedUser: ThothRenameUser): ThothUser {
    val principal = thothPrincipal<ThothPrincipal>()

    if (principal.userId != params.id && !principal.isAdmin) {
        throw ErrorResponse.forbidden("Rename", "account")
    }

    ThothAuthConfig.getUserByUsername(renamedUser.username)?.let {
        throw ErrorResponse.userError("Username already exists")
    }

    var user = ThothAuthConfig.getUserById(params.id) ?: throw ErrorResponse.notFound("User", params.id)
    user = ThothAuthConfig.renameUser(user, params.id)
    return ThothUserImpl.wrap(user)
}
