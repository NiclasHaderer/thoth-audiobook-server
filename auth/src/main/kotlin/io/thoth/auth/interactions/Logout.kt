package io.thoth.auth.interactions

import io.ktor.http.Cookie
import io.ktor.server.application.call
import io.thoth.auth.thothAuthConfig
import io.thoth.openapi.ktor.RouteHandler

interface ThothLogoutParams

fun RouteHandler.logoutUser(
    params: ThothLogoutParams,
    body: Unit,
) {
    val config = thothAuthConfig<Any>()
    call.response.cookies.append(
        Cookie(
            name = "refresh",
            value = "",
            httpOnly = true,
            secure = config.production,
            extensions = mapOf("SameSite" to "Strict", "HttpOnly" to "true", "Secure" to config.ssl.toString()),
            maxAge = 0,
        ),
    )
}
