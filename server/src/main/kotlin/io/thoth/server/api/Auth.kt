package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.auth.interactions.changeUserPassword
import io.thoth.auth.interactions.deleteUser
import io.thoth.auth.interactions.displayUser
import io.thoth.auth.interactions.getJwks
import io.thoth.auth.interactions.getRefreshToken
import io.thoth.auth.interactions.listUsers
import io.thoth.auth.interactions.loginUser
import io.thoth.auth.interactions.logoutUser
import io.thoth.auth.interactions.modifyUserPermissions
import io.thoth.auth.interactions.registerUser
import io.thoth.auth.interactions.renameUser
import io.thoth.auth.models.JWKs
import io.thoth.auth.models.ThothAccessToken
import io.thoth.auth.models.ThothChangePassword
import io.thoth.auth.models.ThothLoginUser
import io.thoth.auth.models.ThothModifyPermissions
import io.thoth.auth.models.ThothRegisterUser
import io.thoth.auth.models.ThothRenameUser
import io.thoth.auth.models.ThothUser
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.delete
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.post
import io.thoth.openapi.ktor.put
import io.thoth.server.common.serializion.kotlin.UUID_S

fun Routing.authRoutes() {
    post<Api.Auth.Login, ThothLoginUser, ThothAccessToken>(RouteHandler::loginUser)

    post<Api.Auth.Logout, Unit, Unit>(RouteHandler::logoutUser)

    post<Api.Auth.Register, ThothRegisterUser, ThothUser<UUID_S>>(RouteHandler::registerUser)

    get<Api.Auth.Jwks, JWKs>(RouteHandler::getJwks)

    put<Api.Auth.User.Id.Permissions, ThothModifyPermissions, ThothUser<UUID_S>>(RouteHandler::modifyUserPermissions)

    get<Api.Auth.User.Id, ThothUser<UUID_S>>(RouteHandler::displayUser)

    get<Api.Auth.User.All, List<ThothUser<UUID_S>>>(RouteHandler::listUsers)

    delete<Api.Auth.User.Id, Unit, Unit>(RouteHandler::deleteUser)

    post<Api.Auth.User.Id.Username, ThothRenameUser, ThothUser<UUID_S>>(RouteHandler::renameUser)

    post<Api.Auth.User.Id.Password, ThothChangePassword, Unit>(RouteHandler::changeUserPassword)

    post<Api.Auth.User.Refresh, Unit, ThothAccessToken>(RouteHandler::getRefreshToken)
}
