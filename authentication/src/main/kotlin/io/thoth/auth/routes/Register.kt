package io.thoth.auth.routes

import io.ktor.http.*
import io.thoth.database.access.getByName
import io.thoth.database.access.toModel
import io.thoth.database.tables.User
import io.thoth.models.UserModel
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

internal fun RouteHandler.register(user: RegisterUser): UserModel {
    val dbUser = User.getByName(user.username)
    if (dbUser != null) {
        serverError(HttpStatusCode.BadRequest, "User already exists")
    }

    val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
    val encodedPassword = encoder.encode(user.password)

    return transaction {
        User.new {
                username = user.username
                passwordHash = encodedPassword
                admin = user.admin
                edit = user.edit
            }
            .toModel()
    }
}
