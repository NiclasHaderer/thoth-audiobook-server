package io.thoth.auth.interactions

import io.ktor.server.application.*
import io.thoth.auth.ThothAuthConfig
import io.thoth.auth.models.ThothAccessToken
import io.thoth.auth.models.ThothAccessTokenImpl
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.auth.utils.generateAccessTokenForUser
import io.thoth.auth.utils.validateJwt
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothRefreshTokenParams

fun RouteHandler.refreshToken(
    params: ThothRefreshTokenParams,
    body: Unit,
): ThothAccessToken {
    val refreshToken = call.request.cookies["refresh"] ?: throw ErrorResponse.unauthorized("No refresh token")
    val decodedJwt = validateJwt(ThothAuthConfig, refreshToken, ThothJwtTypes.Refresh)
    val userId = decodedJwt.getClaim("sub").asString()
    val user = ThothAuthConfig.getUserById(userId) ?: throw ErrorResponse.internalError("User not found")

    return ThothAccessTokenImpl(
        generateAccessTokenForUser(user, ThothAuthConfig),
    )
}
