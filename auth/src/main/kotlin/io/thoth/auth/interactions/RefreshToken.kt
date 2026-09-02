package io.thoth.auth.interactions

import io.ktor.server.routing.RoutingContext
import io.thoth.auth.models.ThothAccessToken
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.generateJwtPairForUser
import io.thoth.auth.utils.validateJwt
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothRefreshTokenParams

fun RoutingContext.refreshAccessToken(
    params: ThothRefreshTokenParams,
    body: Unit,
): ThothAccessToken {
    val refreshToken = call.request.cookies[REFRESH_COOKIE] ?: throw ErrorResponse.unauthorized("No refresh token")
    val config = thothAuthConfig<Any, Any>()
    val user = validateJwt(config, refreshToken, ThothJwtTypes.Refresh)

    val pair = generateJwtPairForUser(user, config)
    call.appendAuthCookies(pair, config)
    return ThothAccessToken(pair.accessToken)
}
