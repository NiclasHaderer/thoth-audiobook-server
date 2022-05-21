package io.thoth.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.thoth.models.UserModel
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import java.util.concurrent.TimeUnit

data class JwtCollection(val bearer: String, val refresh: String)

fun generateJwtForUser(issuer: String, user: UserModel, config: AuthConfig): JwtCollection {
    val keyPair = config.keyPair
    val bearerToken = JWT
        .create()
        .withIssuer(issuer)
        .withKeyId(config.keyId)
        .withClaim("username", user.username)
        .withClaim("edit", user.edit)
        .withClaim("admin", user.admin)
        .withClaim("userId", user.id.toString())
        .withClaim("type", JwtType.Access.name)
        .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))

    val refreshAge = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(60)
    val refreshToken = JWT
        .create()
        .withIssuer(issuer)
        .withKeyId(config.keyId)
        .withClaim("type", JwtType.Refresh.name)
        .withClaim("edit", user.edit)
        .withClaim("admin", user.admin)
        .withClaim("userId", user.id.toString())
        .withClaim("username", user.username)
        .withExpiresAt(Date(refreshAge))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))

    return JwtCollection(bearerToken, refreshToken)
}

enum class JwtType {
    Access,
    Refresh,
}

class ThothPrincipal(
    val payload: Payload,
    val username: String,
    val userId: UUID,
    val edit: Boolean,
    val admin: Boolean,
    val type: JwtType
) : Principal

fun jwtToPrincipal(credentials: JWTCredential): ThothPrincipal? {
    val username = credentials.payload.getClaim("username").asString()
    val edit = credentials.payload.getClaim("edit").asBoolean()
    val admin = credentials.payload.getClaim("admin").asBoolean()
    val userId = credentials.payload.getClaim("userId").asString()
    val enumType = try {
        val type = credentials.payload.getClaim("type").asString()
        JwtType.valueOf(type)
    } catch (e: Exception) {
        return null
    }

    return ThothPrincipal(
        payload = credentials.payload,
        username = username,
        userId = UUID.fromString(userId),
        edit = edit,
        admin = admin,
        type = enumType
    )
}
