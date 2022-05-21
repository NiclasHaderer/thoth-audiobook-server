package io.thoth.auth.routes

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.thoth.auth.ThothPrincipal
import io.thoth.common.exceptions.APINotFound
import io.thoth.common.exceptions.ErrorResponse
import io.thoth.database.tables.User
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
            throw ErrorResponse(HttpStatusCode.BadRequest, "Old name is the same as the new name")
        }
        val existingUser = User.getByName(usernameChange.username)
        if (existingUser != null) {
            throw ErrorResponse(HttpStatusCode.BadRequest, "Username already exits.")
        }
        user.username = usernameChange.username
        user
    }
    call.respond(userModel)
}
