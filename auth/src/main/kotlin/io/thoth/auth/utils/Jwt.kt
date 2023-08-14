package io.thoth.auth.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.thoth.auth.ThothAuthConfig
import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.auth.models.ThothDatabaseUserPermissions
import io.thoth.auth.models.ThothJwtPairImpl
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

fun <ID : Any, PERMISSIONS : ThothDatabaseUserPermissions> generateJwtPairForUser(
    user: ThothDatabaseUser<ID, PERMISSIONS>,
    config: ThothAuthConfig
): ThothJwtPairImpl {
    return ThothJwtPairImpl(
        accessToken = generateAccessTokenForUser(user, config),
        refreshToken = generateRefreshTokenForUser(user, config),
    )
}

internal fun <ID : Any, PERMISSIONS : ThothDatabaseUserPermissions> generateAccessTokenForUser(
    user: ThothDatabaseUser<ID, PERMISSIONS>,
    config: ThothAuthConfig
): String {
    val keyPair = config.keyPairs[config.activeKeyId]!!
    val issuer = config.issuer

    return JWT.create()
        .withIssuer(issuer)
        .withKeyId(config.activeKeyId)
        .also {
            if (config.includePermissionsInJwt) {
                TODO("Serialize JWT permissions")
                // it.withClaim("permissions", user.permissions)
            }
        }
        .withClaim("sub", user.id.toString())
        .withClaim("type", ThothJwtTypes.Access.type)
        .withExpiresAt(Date(System.currentTimeMillis() + config.accessTokenExpiryTime))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))
}

internal fun <ID : Any, PERMISSIONS : ThothDatabaseUserPermissions> generateRefreshTokenForUser(
    user: ThothDatabaseUser<ID, PERMISSIONS>,
    config: ThothAuthConfig
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

fun validateJwt(authConfig: ThothAuthConfig, token: String, type: ThothJwtTypes): DecodedJWT {
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
