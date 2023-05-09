package io.thoth.server.plugins.authentication

import com.auth0.jwt.interfaces.Payload
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

class ThothPrincipal(
    val payload: Payload,
    val username: String,
    val userId: UUID,
    val edit: Boolean,
    val admin: Boolean,
    val type: JwtType
) : Principal

fun jwtToPrincipal(credentials: JWTCredential): ThothPrincipal? {
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
