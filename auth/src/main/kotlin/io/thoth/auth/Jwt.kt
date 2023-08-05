package io.thoth.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

fun generateJwtPairForUser(user: ThothDatabaseUser, config: AuthConfig): ThothJwtPairImpl {
    return ThothJwtPairImpl(
        accessToken = generateAccessTokenForUser(user, config),
        refreshToken = generateRefreshTokenForUser(user, config),
    )
}

internal fun generateAccessTokenForUser(user: ThothDatabaseUser, config: AuthConfig): String {
    val keyPair = config.keyPair[config.activeKeyId]!!
    val issuer = config.issuer

    return JWT.create()
        .withIssuer(issuer)
        .withKeyId(config.activeKeyId)
        .withClaim("sub", user.id.toString())
        .withClaim("type", ThothJwtTypes.Access.type)
        .withExpiresAt(Date(System.currentTimeMillis() + config.accessTokenExpiryTime))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))
}

internal fun generateRefreshTokenForUser(user: ThothDatabaseUser, config: AuthConfig): String {
    val issuer = config.issuer
    val keyPair = config.keyPair[config.activeKeyId]!!

    val refreshAge = System.currentTimeMillis() + config.refreshTokenExpiryTime
    return JWT.create()
        .withIssuer(issuer)
        .withKeyId(config.activeKeyId)
        .withClaim("type", ThothJwtTypes.Refresh.type)
        .withClaim("sub", user.id.toString())
        .withExpiresAt(Date(refreshAge))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))
}
