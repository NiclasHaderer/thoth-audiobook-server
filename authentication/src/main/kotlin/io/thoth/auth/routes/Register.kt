package io.thoth.auth.routes

import io.thoth.database.access.getByName
import io.thoth.database.access.toModel
import io.thoth.database.tables.User
import io.thoth.models.UserModel
import io.thoth.openapi.ErrorResponse
import io.thoth.openapi.RouteHandler
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

class RegisterUser(
    var username: String,
    val password: String,
    var admin: Boolean,
    var edit: Boolean,
)

internal fun RouteHandler.register(user: RegisterUser): UserModel {
    val dbUser = User.getByName(user.username)
    if (dbUser != null) {
        throw ErrorResponse.userError("User with name ${user.username} already exists")
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
