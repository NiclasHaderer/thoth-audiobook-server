package io.thoth.auth.interactions

import io.ktor.http.Cookie
import io.ktor.server.application.ApplicationCall
import io.thoth.auth.ThothAuthConfig

internal const val ACCESS_COOKIE = "access"

// path="/" so the cookie is also sent for the media routes under /api/stream, not just /api/auth.
internal fun ApplicationCall.appendAccessCookie(
    token: String,
    config: ThothAuthConfig<*, *>,
) = response.cookies.append(
    Cookie(
        name = ACCESS_COOKIE,
        value = token,
        httpOnly = true,
        secure = config.ssl,
        path = "/",
        extensions = mapOf("SameSite" to "Strict"),
        maxAge = (config.accessTokenExpiryTime / 1000).toInt(),
    ),
)

internal fun ApplicationCall.clearAccessCookie(config: ThothAuthConfig<*, *>) =
    response.cookies.append(
        Cookie(
            name = ACCESS_COOKIE,
            value = "",
            httpOnly = true,
            secure = config.ssl,
            path = "/",
            extensions = mapOf("SameSite" to "Strict"),
            maxAge = 0,
        ),
    )
