package io.thoth.auth.routes

import io.ktor.http.*
import io.thoth.auth.AuthConfig
import io.thoth.auth.JwtPair
import io.thoth.auth.generateJwtForUser
import io.thoth.database.access.internalGetByName
import io.thoth.database.tables.User
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.serverError
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder


internal fun login(config: AuthConfig): RouteHandler.(user: LoginUser) -> JwtPair {
    return { user ->
        val userModel = User.internalGetByName(user.username) ?: serverError(
            HttpStatusCode.BadRequest,
            "Could not login user"
        )

        val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
        if (!encoder.matches(user.password, userModel.passwordHash)) {
            serverError(HttpStatusCode.BadRequest, "Could not login user")
        }



        generateJwtForUser(config.issuer, userModel, config)
    }
}
