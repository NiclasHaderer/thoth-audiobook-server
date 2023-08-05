package io.thoth.auth.interactions

import io.ktor.http.*
import io.ktor.server.application.*
import io.thoth.auth.AuthConfig
import io.thoth.auth.ThothAccessTokenImpl
import io.thoth.auth.ThothLoginUser
import io.thoth.auth.generateJwtPairForUser
import io.thoth.auth.passwordMatches
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

fun RouteHandler.login(
    loginUser: ThothLoginUser,
): ThothAccessTokenImpl {
    val user =
        AuthConfig.getUserByUsername(loginUser.username)
            ?: throw ErrorResponse.userError(if (AuthConfig.production) "Could not login user" else "Wrong username")

    if (!passwordMatches(loginUser.password, user)) {
        throw ErrorResponse.userError(if (AuthConfig.production) "Could not login user" else "Wrong password")
    }

    val keyPair = generateJwtPairForUser(user, AuthConfig)

    call.response.cookies.append(
        Cookie(
            name = "refresh",
            value = keyPair.refreshToken,
            httpOnly = true,
            secure = AuthConfig.production,
            extensions = mapOf("SameSite" to "Strict", "HostOnly" to "true"),
            maxAge = (AuthConfig.refreshTokenExpiryTime / 1000).toInt(),
        ),
    )

    return ThothAccessTokenImpl(keyPair.accessToken)
}
