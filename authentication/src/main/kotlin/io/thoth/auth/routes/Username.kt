package io.thoth.auth.routes

import io.thoth.auth.thothPrincipal
import io.thoth.database.access.getByName
import io.thoth.database.access.toModel
import io.thoth.database.tables.User
import io.thoth.models.UserModel
import io.thoth.openapi.ErrorResponse
import io.thoth.openapi.RouteHandler
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
