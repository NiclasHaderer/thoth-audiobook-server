package io.thoth.auth.interactions

import io.ktor.http.*
import io.ktor.server.application.*
import io.thoth.auth.ThothAuthConfig
import io.thoth.auth.models.ThothAccessTokenImpl
import io.thoth.auth.models.ThothLoginUser
import io.thoth.auth.utils.generateJwtPairForUser
import io.thoth.auth.utils.passwordMatches
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothLoginParams

fun RouteHandler.login(
    params: ThothLoginParams,
    loginUser: ThothLoginUser,
): ThothAccessTokenImpl {
    val user =
        ThothAuthConfig.getUserByUsername(loginUser.username)
            ?: throw ErrorResponse.userError(
                if (ThothAuthConfig.production) "Could not login user" else "Username does not exist"
            )

    if (!passwordMatches(loginUser.password, user)) {
        throw ErrorResponse.userError(
            if (ThothAuthConfig.production) "Could not login user" else "Password is incorrect"
        )
    }

    val keyPair = generateJwtPairForUser(user, ThothAuthConfig)

    call.response.cookies.append(
        Cookie(
            name = "refresh",
            value = keyPair.refreshToken,
            httpOnly = true,
            secure = ThothAuthConfig.production,
            extensions = mapOf("SameSite" to "Strict", "HostOnly" to "true"),
            maxAge = (ThothAuthConfig.refreshTokenExpiryTime / 1000).toInt(),
        ),
    )

    return ThothAccessTokenImpl(keyPair.accessToken)
}
