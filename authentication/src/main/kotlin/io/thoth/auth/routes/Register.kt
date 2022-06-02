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
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

fun Route.registerEndpoint(config: AuthConfig) = post {
    val user = call.receive<RegisterUser>()
    val dbUser = User.getByName(user.username)
    if (dbUser != null) {
        serverError(HttpStatusCode.BadRequest, "User already exists")
    }

    val encoder = Argon2PasswordEncoder()
    val encodedPassword = encoder.encode(user.password)
    val userModel = transaction {
        User.new {
            username = user.username
            passwordHash = encodedPassword
            admin = user.admin
            edit = user.edit
        }.toModel()
    }

    val jwtPair = generateJwtForUser(config.issuer, userModel, config)
    call.respond(jwtPair)
}
