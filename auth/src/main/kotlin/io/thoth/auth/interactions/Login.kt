package io.thoth.auth.interactions

import io.ktor.http.*
import io.ktor.server.application.*
import io.thoth.auth.models.ThothAccessTokenImpl
import io.thoth.auth.models.ThothLoginUser
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.generateJwtPairForUser
import io.thoth.auth.utils.passwordMatches
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothLoginParams

fun RouteHandler.login(
    params: ThothLoginParams,
    loginUser: ThothLoginUser,
): ThothAccessTokenImpl {
    val config = thothAuthConfig()

    val user =
        config.getUserByUsername(loginUser.username)
            ?: throw ErrorResponse.userError(
                if (config.production) "Could not login user" else "Username does not exist"
            )

    if (!passwordMatches(loginUser.password, user)) {
        throw ErrorResponse.userError(if (config.production) "Could not login user" else "Password is incorrect")
    }

    val keyPair = generateJwtPairForUser(user, config)

    call.response.cookies.append(
        Cookie(
            name = "refresh",
            value = keyPair.refreshToken,
            httpOnly = true,
            secure = config.production,
            extensions = mapOf("SameSite" to "Strict", "HostOnly" to "true"),
            maxAge = (config.refreshTokenExpiryTime / 1000).toInt(),
        ),
    )

    return ThothAccessTokenImpl(keyPair.accessToken)
}
