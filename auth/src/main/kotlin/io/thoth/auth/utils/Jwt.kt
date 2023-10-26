package io.thoth.auth.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.thoth.auth.ThothAuthConfig
import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.auth.models.ThothJwtPair
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.auth.models.ThothUserPermissions
import io.thoth.auth.models.impl.ThothJwtPairImpl
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

fun <ID : Any, PERMISSIONS : ThothUserPermissions> generateJwtPairForUser(
    user: ThothDatabaseUser<ID, PERMISSIONS>,
    config: ThothAuthConfig<ID, PERMISSIONS>
): ThothJwtPair {
    return ThothJwtPairImpl(
        accessToken = generateAccessTokenForUser(user, config),
        refreshToken = generateRefreshTokenForUser(user, config),
    )
}

internal fun <ID : Any, PERMISSIONS : ThothUserPermissions> generateAccessTokenForUser(
    user: ThothDatabaseUser<ID, PERMISSIONS>,
    config: ThothAuthConfig<ID, PERMISSIONS>
): String {
    val keyPair = config.keyPairs[config.activeKeyId]!!
    val issuer = config.issuer

    return JWT.create()
        .withIssuer(issuer)
        .withKeyId(config.activeKeyId)
        .withClaim("permissions", config.serializePermissions(user.permissions))
        .withClaim("sub", user.id.toString())
        .withClaim("type", ThothJwtTypes.Access.type)
        .withExpiresAt(Date(System.currentTimeMillis() + config.accessTokenExpiryTime))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))
}

internal fun <ID : Any, PERMISSIONS : ThothUserPermissions> generateRefreshTokenForUser(
    user: ThothDatabaseUser<ID, PERMISSIONS>,
    config: ThothAuthConfig<ID, PERMISSIONS>
): String {
    val issuer = config.issuer
    val keyPair = config.keyPairs[config.activeKeyId]!!

    val refreshAge = System.currentTimeMillis() + config.refreshTokenExpiryTime
    return JWT.create()
        .withIssuer(issuer)
        .withKeyId(config.activeKeyId)
        .withClaim("type", ThothJwtTypes.Refresh.type)
        .withClaim("sub", user.id.toString())
        .withExpiresAt(Date(refreshAge))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))
}

fun validateJwt(authConfig: ThothAuthConfig<*, *>, token: String, type: ThothJwtTypes): DecodedJWT {
    val decodedJWT = JWT.decode(token)
    if (decodedJWT.algorithm != "RS256") {
        throw ErrorResponse.userError("Unsupported JWT algorithm ${decodedJWT.algorithm}")
    }

    val provider = authConfig.jwkProvider
    val algorithm = Algorithm.RSA256(provider[decodedJWT.keyId].publicKey as RSAPublicKey, null)
    val verifier = JWT.require(algorithm).withIssuer(authConfig.issuer).build()

    runCatching { verifier.verify(decodedJWT) }
        .onFailure { throw ErrorResponse.unauthorized("Invalid JWT: ${it.message}") }

    // Make sure that the token is of the correct type
    if (decodedJWT.getClaim("type").asString() != type.type) {
        throw ErrorResponse.unauthorized("Invalid JWT type")
    }

    return decodedJWT
}
