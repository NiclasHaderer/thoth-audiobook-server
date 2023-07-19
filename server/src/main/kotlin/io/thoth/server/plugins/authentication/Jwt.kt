package io.thoth.server.plugins.authentication

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.http.*
import io.thoth.models.InternalUserModel
import io.thoth.openapi.openapi.errors.ErrorResponse
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import java.util.concurrent.TimeUnit

class JwtPair(val access: String, val refresh: String)

enum class JwtType(val type: String) {
    Access("access"),
    Refresh("refresh")
}

fun generateJwtForUser(user: InternalUserModel, config: AuthConfig): JwtPair {
    return JwtPair(
        access = generateAccessTokenForUser(user, config),
        refresh = generateRefreshTokenForUser(user, config),
    )
}

val ACCESS_TOKEN_EXPIRY_MS = TimeUnit.MINUTES.toMillis(5)

fun generateAccessTokenForUser(user: InternalUserModel, config: AuthConfig): String {
    val keyPair = config.keyPair
    val issuer = config.issuer
    return JWT.create()
        .withIssuer(issuer)
        .withKeyId(config.keyId)
        .withClaim("username", user.username)
        .withClaim("edit", user.edit)
        .withClaim("admin", user.admin)
        .withClaim("sub", user.id.toString())
        .withClaim("type", JwtType.Access.type)
        .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY_MS))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))
}

val REFRESH_TOKEN_EXPIRY_MS = TimeUnit.DAYS.toMillis(60)

fun generateRefreshTokenForUser(user: InternalUserModel, config: AuthConfig): String {
    val keyPair = config.keyPair
    val issuer = config.issuer

    val refreshAge = System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY_MS
    return JWT.create()
        .withIssuer(issuer)
        .withKeyId(config.keyId)
        .withClaim("type", JwtType.Refresh.type)
        .withClaim("sub", user.id.toString())
        .withExpiresAt(Date(refreshAge))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))
}

fun validateJwt(authConfig: AuthConfig, token: String, type: JwtType): DecodedJWT {
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
