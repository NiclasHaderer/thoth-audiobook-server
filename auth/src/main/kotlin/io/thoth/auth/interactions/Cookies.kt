package io.thoth.auth.interactions

import io.ktor.http.Cookie
import io.ktor.server.application.ApplicationCall
import io.thoth.auth.ThothAuthConfig
import io.thoth.auth.models.ThothJwtPair

internal const val ACCESS_COOKIE = "access"
internal const val REFRESH_COOKIE = "refresh"

// Clearing a cookie only works if every attribute matches the one that set it, so both go through here.
// The access cookie is scoped to "/" because the media routes under /api/stream need it too, not just /api/auth.
private fun ApplicationCall.appendAuthCookie(
    name: String,
    value: String,
    lifetimeMillis: Long,
    config: ThothAuthConfig<*, *>,
) = response.cookies.append(
    Cookie(
        name = name,
        value = value,
        httpOnly = true,
        secure = config.ssl,
        path = if (name == ACCESS_COOKIE) "/" else null,
        extensions = mapOf("SameSite" to "Strict"),
        maxAge = (lifetimeMillis / 1000).toInt(),
    ),
)

internal fun ApplicationCall.appendAuthCookies(
    pair: ThothJwtPair,
    config: ThothAuthConfig<*, *>,
) {
    appendAuthCookie(ACCESS_COOKIE, pair.accessToken, config.accessTokenExpiryTime, config)
    appendAuthCookie(REFRESH_COOKIE, pair.refreshToken, config.refreshTokenExpiryTime, config)
}

internal fun ApplicationCall.clearAuthCookies(config: ThothAuthConfig<*, *>) {
    appendAuthCookie(ACCESS_COOKIE, "", 0, config)
    appendAuthCookie(REFRESH_COOKIE, "", 0, config)
}
