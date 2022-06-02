package io.thoth.auth.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.thoth.auth.AuthConfig
import io.thoth.auth.generateJwtForUser
import io.thoth.database.tables.User
import io.thoth.openapi.serverError
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder


fun Route.loginEndpoint(config: AuthConfig) = post {
    val user = call.receive<LoginUser>()

    val userModel = User.getByName(user.username) ?: serverError(
        HttpStatusCode.BadRequest,
        "Could not login user"
    )

    val encoder = Argon2PasswordEncoder()
    if (!encoder.matches(user.password, userModel.passwordHash)) {
        serverError(HttpStatusCode.BadRequest, "Could not login user")
    }

    val jwtPair = generateJwtForUser(config.issuer, userModel, config)
    call.respond(jwtPair)
}
