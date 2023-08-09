package io.thoth.auth.interactions

import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserImpl
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothDisplayUserParams<T : Any> {
    val id: T
}

fun <T : Any> RouteHandler.displayUser(params: ThothDisplayUserParams<T>): ThothUser<T> {
    val principal = thothPrincipal<ThothPrincipal<T>>()
    val config = thothAuthConfig()

    if (principal.userId != params.id && !principal.isAdmin) {
        throw ErrorResponse.forbidden("View", "account")
    }

    val user = config.getUserById(params.id) ?: throw ErrorResponse.notFound("User", params.id)
    return ThothUserImpl.wrap(user) as ThothUser<T>
}
