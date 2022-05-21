package io.thoth.auth.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.thoth.auth.AuthConfig
import io.thoth.auth.generateJwtForUser
import io.thoth.common.exceptions.ErrorResponse
import io.thoth.database.tables.User
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder


fun Route.loginEndpoint(config: AuthConfig) = post {
    val user = call.receive<LoginUser>()

    val userModel = User.getByName(user.username) ?: throw ErrorResponse(
        HttpStatusCode.BadRequest,
        "Could not login user"
    )

    val encoder = Argon2PasswordEncoder()
    if (!encoder.matches(user.password, userModel.passwordHash)) {
        throw ErrorResponse(HttpStatusCode.BadRequest, "Could not login user")
    }

    val jwtPair = generateJwtForUser(config.issuer, userModel, config)
    call.respond(jwtPair)
}
