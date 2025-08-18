package io.thoth.auth.interactions

import io.ktor.server.routing.RoutingContext
import io.thoth.auth.models.ThothAccessToken
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.generateAccessTokenForUser
import io.thoth.auth.utils.validateJwt
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.util.UUID

interface ThothRefreshTokenParams

fun RoutingContext.getRefreshToken(
    params: ThothRefreshTokenParams,
    body: Unit,
): ThothAccessToken {
    val refreshToken = call.request.cookies["refresh"] ?: throw ErrorResponse.unauthorized("No refresh token")
    val config = thothAuthConfig<Any, Any>()
    val decodedJwt = validateJwt(config, refreshToken, ThothJwtTypes.Refresh)
    val userIdStr = decodedJwt.getClaim("sub").asString()
    val userId = UUID.fromString(userIdStr)
    val user = config.getUserById(userId) ?: throw ErrorResponse.internalError("User not found")

    return ThothAccessToken(generateAccessTokenForUser(user, config))
}
