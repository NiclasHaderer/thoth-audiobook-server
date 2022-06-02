package io.thoth.auth.routes

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.database.tables.User
import io.thoth.models.UserModel
import io.thoth.openapi.routing.post
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

fun Route.registerEndpoint() = post<Unit, RegisterUser, UserModel> { _, user ->
    val dbUser = User.getByName(user.username)
    if (dbUser != null) {
        serverError(HttpStatusCode.BadRequest, "User already exists")
    }

    val encoder = Argon2PasswordEncoder()
    val encodedPassword = encoder.encode(user.password)

    transaction {
        User.new {
            username = user.username
            passwordHash = encodedPassword
            admin = user.admin
            edit = user.edit
        }.toModel()
    }

}
