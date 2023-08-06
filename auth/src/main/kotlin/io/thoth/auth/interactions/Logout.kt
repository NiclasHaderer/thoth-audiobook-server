package io.thoth.auth.interactions

import io.ktor.http.*
import io.ktor.server.application.*
import io.thoth.auth.ThothAuthConfig
import io.thoth.openapi.ktor.RouteHandler

interface ThothLogoutParams

fun RouteHandler.logout(
    params: ThothLogoutParams,
    body: Unit,
) {
    call.response.cookies.append(
        Cookie(
            name = "refresh",
            value = "",
            httpOnly = true,
            secure = ThothAuthConfig.production,
            extensions = mapOf("SameSite" to "Strict", "HostOnly" to "true"),
            maxAge = 0,
        ),
    )
}
