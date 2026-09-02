package io.thoth.auth.interactions

import io.ktor.server.routing.RoutingContext
import io.thoth.auth.models.ThothAccessToken
import io.thoth.auth.models.ThothLoginUser
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.generateJwtPairForUser
import io.thoth.auth.utils.passwordMatches
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothLoginParams

fun RoutingContext.loginUser(
    params: ThothLoginParams,
    loginUser: ThothLoginUser,
): ThothAccessToken {
    val config = thothAuthConfig<Any, Any>()

    // Never reveal whether the username or the password was wrong
    val user = config.getUserByUsername(loginUser.username) ?: throw ErrorResponse.userError("Could not login user")

    if (!passwordMatches(loginUser.password, user)) {
        throw ErrorResponse.userError("Could not login user")
    }

    val keyPair = generateJwtPairForUser(user, config)
    call.appendAuthCookies(keyPair, config)

    return ThothAccessToken(keyPair.accessToken)
}
