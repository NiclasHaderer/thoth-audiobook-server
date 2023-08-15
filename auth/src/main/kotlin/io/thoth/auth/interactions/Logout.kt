package io.thoth.auth.interactions

import io.ktor.http.*
import io.ktor.server.application.*
import io.thoth.auth.models.ThothUserPermissions
import io.thoth.auth.thothAuthConfig
import io.thoth.openapi.ktor.RouteHandler

interface ThothLogoutParams

fun RouteHandler.logoutUser(
    params: ThothLogoutParams,
    body: Unit,
) {
    val config = thothAuthConfig<Any, ThothUserPermissions>()
    call.response.cookies.append(
        Cookie(
            name = "refresh",
            value = "",
            httpOnly = true,
            secure = config.production,
            extensions = mapOf("SameSite" to "Strict", "HostOnly" to "true"),
            maxAge = 0,
        ),
    )
}
