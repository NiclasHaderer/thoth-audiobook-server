package io.thoth.server.authentication.routes

import io.thoth.generators.openapi.RouteHandler
import io.thoth.generators.openapi.errors.ErrorResponse
import io.thoth.models.UserModel
import io.thoth.server.authentication.thothPrincipal
import io.thoth.server.database.access.getByName
import io.thoth.server.database.access.toModel
import io.thoth.server.database.tables.User
import org.jetbrains.exposed.sql.transactions.transaction

class UsernameChange(val username: String)

internal fun RouteHandler.changeUsername(usernameChange: UsernameChange): UserModel {
    val principal = thothPrincipal()

    return transaction {
        val user = User.findById(principal.userId) ?: throw ErrorResponse.notFound("User", principal.userId)
        val existingUser = User.getByName(usernameChange.username)
        if (existingUser != null) {
            throw ErrorResponse.userError("User with name ${usernameChange.username} already exists")
        }
        user.username = usernameChange.username
        user.toModel()
    }
}
