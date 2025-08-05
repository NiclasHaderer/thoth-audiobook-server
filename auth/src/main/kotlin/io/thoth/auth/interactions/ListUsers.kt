package io.thoth.auth.interactions

import io.thoth.auth.models.ThothUser
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.auth.utils.wrap
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothListUserParams

fun RouteHandler.listUsers(
    params: ThothListUserParams
): List<ThothUser> {
    val config = thothAuthConfig<Any>()

    val principal = thothPrincipal<ThothPrincipal>()
    if (!config.isAdmin(principal)) {
        throw ErrorResponse.userError("User is not admin")
    }

    return config.listAllUsers().map { it.wrap() }
}
