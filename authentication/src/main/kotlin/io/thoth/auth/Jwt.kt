package io.thoth.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import java.util.concurrent.TimeUnit

data class JwtCollection(val bearer: String, val refresh: String)

fun generateJwtForUser(issuer: String, user: String, keyPair: KeyPair): JwtCollection {
    val bearerToken = JWT
        .create()
        .withIssuer(issuer)
        .withClaim("type", JwtType.Access.value)
        .withClaim("username", user)
        .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))

    val refreshAge = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(60)
    val refreshToken = JWT
        .create()
        .withIssuer(issuer)
        .withClaim("type", JwtType.Refresh.value)
        .withClaim("username", user)
        .withExpiresAt(Date(refreshAge))
        .sign(Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey))

    return JwtCollection(bearerToken, refreshToken)
}

enum class JwtType(val value: String) {
    Access("access"),
    Refresh("refresh"),
}

class ThothPrincipal(
    val payload: Payload,
    // TODO check if user id and user name can be inferred from the created jwt
    val username: String,
    val edit: Boolean,
    val admin: Boolean,
    val userId: UUID,
    val type: JwtType
) : Principal

fun jwtToPrincipal(credentials: JWTCredential): ThothPrincipal {
    val username = credentials.payload.getClaim("username").asString()
    val edit = credentials.payload.getClaim("edit").asBoolean()
    val admin = credentials.payload.getClaim("admin").asBoolean()
    val type = credentials.payload.getClaim("type").asString()
    val id = credentials.payload.getClaim("id").asString()

    return ThothPrincipal(
        payload = credentials.payload,
        username = username,
        edit = edit,
        admin = admin,
        userId = UUID.fromString(id),
        type = JwtType.Access // TODO real type
    )
}
