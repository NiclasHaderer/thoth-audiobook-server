package io.thoth.auth.routes

import io.ktor.http.*
import io.thoth.auth.thothPrincipal
import io.thoth.database.access.getById
import io.thoth.database.access.getByName
import io.thoth.database.access.toModel
import io.thoth.database.tables.User
import io.thoth.models.UserModel
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction

internal fun RouteHandler.getUser(): UserModel {
  val principal = thothPrincipal()
  return User.getById(principal.userId)
      ?: serverError(HttpStatusCode.NotFound, "Could not find user")
}

internal fun RouteHandler.changeUsername(usernameChange: UsernameChange): UserModel {
  val principal = thothPrincipal()

  return transaction {
    val user =
        User.findById(principal.userId)
            ?: serverError(HttpStatusCode.NotFound, "Could not find user")
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
