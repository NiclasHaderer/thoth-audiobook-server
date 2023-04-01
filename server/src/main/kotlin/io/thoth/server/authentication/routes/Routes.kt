package io.thoth.server.authentication.routes

import io.thoth.models.UserModel
import io.thoth.openapi.RouteHandler
import io.thoth.server.authentication.AuthConfigImpl
import io.thoth.server.authentication.JwtPair
import java.util.*

interface AuthRoutes {
    val login: RouteHandler.(user: LoginUser) -> JwtPair
    val register: RouteHandler.(RegisterUser) -> UserModel
    val jwks: RouteHandler.() -> JwksResponse
    val modify: RouteHandler.(UUID, ModifyUser) -> UserModel
    val getUser: RouteHandler.() -> UserModel
    val delete: RouteHandler.() -> Unit
    val changeUsername: RouteHandler.(UsernameChange) -> UserModel
    val changePassword: RouteHandler.(PasswordChange) -> Unit
}

internal fun authRoutes(config: AuthConfigImpl): AuthRoutes {
    return object : AuthRoutes {
        override val login = login(config)
        override val jwks = jwksEndpoint(config)
        override val modify = RouteHandler::modifyUser
        override val register = RouteHandler::register
        override val getUser = RouteHandler::getUser
        override val delete = RouteHandler::deleteUser
        override val changeUsername = RouteHandler::changeUsername
        override val changePassword = RouteHandler::changePassword
    }
}
