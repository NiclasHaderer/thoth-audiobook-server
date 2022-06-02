package io.thoth.auth.routes

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.auth.thothPrincipal
import io.thoth.common.exceptions.APINotFound
import io.thoth.database.tables.User
import io.thoth.models.UserModel
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.post
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction


internal fun Route.getUser() = get<Unit, UserModel> {
    val principal = thothPrincipal()
    transaction {
        User.getById(principal.userId).toPublicModel()
    }
}

internal fun Route.changeUsername() = post<Unit, UsernameChange, UserModel> { _, usernameChange ->
    val principal = thothPrincipal()

    transaction {
        val user = User.findById(principal.userId) ?: throw APINotFound("Could not find user")
        if (user.username === usernameChange.username) {
            serverError(HttpStatusCode.BadRequest, "Old name is the same as the new name")
        }
        val existingUser = User.getByName(usernameChange.username)
        if (existingUser != null) {
            serverError(HttpStatusCode.BadRequest, "Username already exits.")
        }
        user.username = usernameChange.username
        user.toModel()
    }
}
