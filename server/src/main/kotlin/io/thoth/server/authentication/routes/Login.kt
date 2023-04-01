package io.thoth.server.authentication.routes

import io.thoth.openapi.ErrorResponse
import io.thoth.openapi.RouteHandler
import io.thoth.server.authentication.AuthConfigImpl
import io.thoth.server.authentication.JwtPair
import io.thoth.server.authentication.generateJwtForUser
import io.thoth.server.database.access.internalGetByName
import io.thoth.server.database.tables.User
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

class LoginUser(
    var username: String,
    val password: String,
)

internal fun login(config: AuthConfigImpl): RouteHandler.(user: LoginUser) -> JwtPair {
    return { user ->
        val userModel = User.internalGetByName(user.username) ?: throw ErrorResponse.userError("Could not login user")

        val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
        if (!encoder.matches(user.password, userModel.passwordHash)) {
            throw ErrorResponse.userError("Could not login user")
        }

        generateJwtForUser(config.issuer, userModel, config)
    }
}
