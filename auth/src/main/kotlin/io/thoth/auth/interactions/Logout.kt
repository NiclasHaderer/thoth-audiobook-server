package io.thoth.auth.interactions

import io.ktor.http.*
import io.ktor.server.application.*
import io.thoth.auth.AuthConfig
import io.thoth.openapi.ktor.RouteHandler

fun RouteHandler.logout() {
    call.response.cookies.append(
        Cookie(
            name = "refresh",
            value = "",
            httpOnly = true,
            secure = AuthConfig.production,
            extensions = mapOf("SameSite" to "Strict", "HostOnly" to "true"),
            maxAge = 0,
        ),
    )
}
