package io.thoth.auth

import io.thoth.openapi.ktor.RouteHandler
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

internal fun passwordMatches(password: String, user: ThothDatabaseUser): Boolean {
    val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
    return encoder.matches(password, user.password)
}

fun RouteHandler.password() {}
