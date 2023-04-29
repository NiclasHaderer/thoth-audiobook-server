package io.thoth.server.authentication.routes

import io.thoth.generators.openapi.RouteHandler
import io.thoth.generators.openapi.errors.ErrorResponse
import io.thoth.models.UserModel
import io.thoth.server.database.access.getByName
import io.thoth.server.database.access.toModel
import io.thoth.server.database.tables.User
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

class RegisterUser(
    var username: String,
    val password: String,
    var admin: Boolean,
    var edit: Boolean,
)

internal fun RouteHandler.register(user: RegisterUser): UserModel = transaction {
    // TODO fix that every user can simply register as admin
    val dbUser = User.getByName(user.username)
    if (dbUser != null) {
        throw ErrorResponse.userError("User with name ${user.username} already exists")
    }

    val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
    val encodedPassword = encoder.encode(user.password)

    User.new {
            username = user.username
            passwordHash = encodedPassword
            admin = user.admin
            edit = user.edit
            changePassword = false
        }
        .toModel()
}
