package io.thoth.auth.interactions

import io.ktor.http.Cookie
import io.ktor.server.routing.RoutingContext
import io.thoth.auth.thothAuthConfig

interface ThothLogoutParams

fun RoutingContext.logoutUser(
    params: ThothLogoutParams,
    body: Unit,
) {
    val config = thothAuthConfig<Any, Any>()
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
