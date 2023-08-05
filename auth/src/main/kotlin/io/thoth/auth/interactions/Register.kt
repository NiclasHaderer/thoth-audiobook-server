package io.thoth.auth.interactions

import io.thoth.auth.AuthConfig
import io.thoth.auth.RegisteredUserImpl
import io.thoth.auth.ThothRegisterUser
import io.thoth.auth.encodePassword
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

fun RouteHandler.register(user: ThothRegisterUser) {
    val dbUser = AuthConfig.getUserByUsername(user.username)
    if (dbUser != null) {
        throw ErrorResponse.userError("User with name ${user.username} already exists")
    }

    val encodedPassword = encodePassword(user.password)
    val isAdmin = AuthConfig.firstUserIsAdmin && AuthConfig.isFirstUser()

    AuthConfig.createUser(
        RegisteredUserImpl(
            username = user.username,
            passwordHash = encodedPassword,
            admin = isAdmin,
        ),
    )
}
