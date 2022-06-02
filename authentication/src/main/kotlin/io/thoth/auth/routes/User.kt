package io.thoth.auth.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.thoth.auth.ThothPrincipal
import io.thoth.common.exceptions.APINotFound
import io.thoth.database.tables.User
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction


internal fun Route.getUser() = get {
    val principal = call.principal<ThothPrincipal>()!!
    val userModel = transaction {
        User.getById(principal.userId).toPublicModel()
    }

    call.respond(userModel)
}

internal fun Route.changeUsername() = get {
    val principal = call.principal<ThothPrincipal>()!!
    val usernameChange = call.receive<UsernameChange>()

    val userModel = transaction {
        val user = User.findById(principal.userId) ?: throw APINotFound("Could not find user")
        if (user.username === usernameChange.username) {
            serverError(HttpStatusCode.BadRequest, "Old name is the same as the new name")
        }
        val existingUser = User.getByName(usernameChange.username)
        if (existingUser != null) {
            serverError(HttpStatusCode.BadRequest, "Username already exits.")
        }
        user.username = usernameChange.username
        user
    }
    call.respond(userModel)
}
