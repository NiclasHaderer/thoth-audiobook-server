package io.thoth.auth.interactions

import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserImpl
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothListUserParams

fun <T : Any> RouteHandler.listUsers(
    params: ThothListUserParams,
): List<ThothUser<T>> {
    val principal = thothPrincipal<ThothPrincipal<T>>()
    if (!principal.isAdmin) {
        throw ErrorResponse.userError("User is not admin")
    }

    val config = thothAuthConfig()
    return config.listAllUsers().map { ThothUserImpl.wrap(it) } as List<ThothUser<T>>
}
