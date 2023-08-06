package io.thoth.auth.interactions

import io.thoth.auth.models.RegisteredUserImpl
import io.thoth.auth.models.ThothRegisterUser
import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserImpl
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.hashPassword
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothRegisterParams

fun RouteHandler.register(params: ThothRegisterParams, user: ThothRegisterUser): ThothUser {
    val config = thothAuthConfig()
    val dbUser = config.getUserByUsername(user.username)
    if (dbUser != null) {
        throw ErrorResponse.userError("User with name ${user.username} already exists")
    }

    val passwordHash = hashPassword(user.password)
    val isAdmin = config.firstUserIsAdmin && config.isFirstUser()

    return config
        .createUser(
            RegisteredUserImpl(
                username = user.username,
                passwordHash = passwordHash,
                admin = isAdmin,
            ),
        )
        .let { ThothUserImpl.wrap(it) }
}
