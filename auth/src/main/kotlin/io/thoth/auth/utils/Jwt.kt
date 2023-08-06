package io.thoth.auth.utils

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.http.*
import io.thoth.auth.ThothAuthConfig
import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.auth.models.ThothJwtPairImpl
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import java.util.concurrent.TimeUnit

fun generateJwtPairForUser(user: ThothDatabaseUser, config: ThothAuthConfig): ThothJwtPairImpl {
    return ThothJwtPairImpl(
        accessToken = generateAccessTokenForUser(user, config),
        refreshToken = generateRefreshTokenForUser(user, config),
    )
}

internal fun generateAccessTokenForUser(user: ThothDatabaseUser, config: ThothAuthConfig): String {
    val keyPair = ThothAuthConfig.keyPairs[ThothAuthConfig.activeKeyId]!!
    val issuer = ThothAuthConfig.issuer

    return JWT.create()
        .withIssuer(issuer)
        .withKeyId(ThothAuthConfig.activeKeyId)
        .withClaim("sub", user.id.toString())
        .withClaim("type", ThothJwtTypes.Access.type)
        .withExpiresAt(Date(System.currentTimeMillis() + ThothAuthConfig.accessTokenExpiryTime))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))
}

internal fun generateRefreshTokenForUser(user: ThothDatabaseUser, config: ThothAuthConfig): String {
    val issuer = ThothAuthConfig.issuer
    val keyPair = ThothAuthConfig.keyPairs[ThothAuthConfig.activeKeyId]!!

    val refreshAge = System.currentTimeMillis() + ThothAuthConfig.refreshTokenExpiryTime
    return JWT.create()
        .withIssuer(issuer)
        .withKeyId(ThothAuthConfig.activeKeyId)
        .withClaim("type", ThothJwtTypes.Refresh.type)
        .withClaim("sub", user.id.toString())
        .withExpiresAt(Date(refreshAge))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))
}

fun validateJwt(authConfig: ThothAuthConfig, token: String, type: ThothJwtTypes): DecodedJWT {
    val url =
        URLBuilder()
            .apply {
                protocol = authConfig.protocol
                host = authConfig.domain
                encodedPath = authConfig.jwksPath
            }
            .build()
            .toURI()
            .toURL()
    val provider = JwkProviderBuilder(url).cached(10, 24, TimeUnit.HOURS).rateLimited(10, 1, TimeUnit.MINUTES).build()
    val decodedJWT = JWT.decode(token)

    if (decodedJWT.algorithm != "RS256") {
        throw ErrorResponse.userError("Unsupported JWT algorithm ${decodedJWT.algorithm}")
    }
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
