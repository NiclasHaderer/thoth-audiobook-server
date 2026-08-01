package io.thoth.auth.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.thoth.auth.ThothAuthConfig
import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.auth.models.ThothJwtPair
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Date

fun generateJwtPairForUser(
    user: ThothDatabaseUser,
    config: ThothAuthConfig<*, *>,
): ThothJwtPair =
    ThothJwtPair(
        accessToken = generateAccessTokenForUser(user, config),
        refreshToken = generateRefreshTokenForUser(user, config),
    )

internal fun generateAccessTokenForUser(
    user: ThothDatabaseUser,
    config: ThothAuthConfig<*, *>,
): String {
    val keyPair = config.keyPairs[config.activeKeyId]!!
    val issuer = config.issuer

    return JWT
        .create()
        .withIssuer(issuer)
        .withKeyId(config.activeKeyId)
        .withClaim("sub", user.id.toString())
        .withClaim("type", ThothJwtTypes.Access.type)
        .withExpiresAt(Date(System.currentTimeMillis() + config.accessTokenExpiryTime))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))
}

internal fun generateRefreshTokenForUser(
    user: ThothDatabaseUser,
    config: ThothAuthConfig<*, *>,
): String {
    val issuer = config.issuer
    val keyPair = config.keyPairs[config.activeKeyId]!!

    val refreshAge = System.currentTimeMillis() + config.refreshTokenExpiryTime
    return JWT
        .create()
        .withIssuer(issuer)
        .withKeyId(config.activeKeyId)
        .withClaim("type", ThothJwtTypes.Refresh.type)
        .withClaim("sub", user.id.toString())
        .withExpiresAt(Date(refreshAge))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))
}

fun validateJwt(
    authConfig: ThothAuthConfig<*, *>,
    token: String,
    type: ThothJwtTypes,
): DecodedJWT {
    val decodedJWT = JWT.decode(token)
    if (decodedJWT.algorithm != "RS256") {
        throw ErrorResponse.userError("Unsupported JWT algorithm ${decodedJWT.algorithm}")
    }

    val verifier =
        authConfig.verifierFor(decodedJWT.keyId) ?: throw ErrorResponse.unauthorized("Unknown JWT key id")

    runCatching { verifier.verify(decodedJWT) }
        .onFailure { throw ErrorResponse.unauthorized("Invalid JWT: ${it.message}") }

    // Make sure that the token is of the correct type
    if (decodedJWT.getClaim("type").asString() != type.type) {
        throw ErrorResponse.unauthorized("Invalid JWT type")
    }

    return decodedJWT
}
