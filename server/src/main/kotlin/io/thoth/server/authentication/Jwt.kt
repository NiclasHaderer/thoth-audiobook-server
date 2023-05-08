package io.thoth.server.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.thoth.models.InternalUserModel
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import java.util.concurrent.TimeUnit

class JwtPair(val access: String, val refresh: String)

enum class JwtType(val type: String) {
    Access("access"),
    Refresh("refresh")
}

class ThothPrincipal(
    val payload: Payload,
    val username: String,
    val userId: UUID,
    val edit: Boolean,
    val admin: Boolean,
    val type: JwtType
) : Principal

internal fun generateJwtForUser(issuer: String, user: InternalUserModel, config: AuthConfigImpl): JwtPair {
    val keyPair = config.keyPair
    val bearerToken =
        JWT.create()
            .withIssuer(issuer)
            .withKeyId(config.keyId)
            .withClaim("username", user.username)
            .withClaim("edit", user.edit)
            .withClaim("admin", user.admin)
            .withClaim("sub", user.id.toString())
            .withClaim("type", JwtType.Access.type)
            .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)))
            .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))

    val refreshAge = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(60)
    val refreshToken =
        JWT.create()
            .withIssuer(issuer)
            .withKeyId(config.keyId)
            .withClaim("type", JwtType.Refresh.type)
            .withClaim("sub", user.id.toString())
            .withExpiresAt(Date(refreshAge))
            .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))

    return JwtPair(bearerToken, refreshToken)
}

internal fun jwtToPrincipal(credentials: JWTCredential): ThothPrincipal? {
    val username = credentials.payload.getClaim("username").asString() ?: return null
    val edit = credentials.payload.getClaim("edit").asBoolean() ?: return null
    val admin = credentials.payload.getClaim("admin").asBoolean() ?: return null
    val userId = credentials.payload.getClaim("sub").asString() ?: return null
    val enumType =
        try {
            val type = credentials.payload.getClaim("type").asString() ?: return null
            JwtType.values().first { it.type == type }
        } catch (e: Exception) {
            return null
        }

    return ThothPrincipal(
        payload = credentials.payload,
        username = username,
        userId = UUID.fromString(userId),
        edit = edit,
        admin = admin,
        type = enumType,
    )
}
