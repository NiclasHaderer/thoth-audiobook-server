package io.thoth.auth.interactions

import io.thoth.auth.ThothAuthConfig
import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserImpl
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothListAllParams

fun RouteHandler.listAll(
    params: ThothListAllParams,
    body: Unit,
): List<ThothUser> {
    val principal = thothPrincipal<ThothPrincipal>()
    if (!principal.isAdmin) {
        throw ErrorResponse.userError("User is not admin")
    }

    return ThothAuthConfig.listAllUsers().map { ThothUserImpl.wrap(it) }
}
