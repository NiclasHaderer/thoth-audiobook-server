package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.auth.interactions.changeUserPassword
import io.thoth.auth.interactions.currentUser
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
import io.thoth.models.UserPermissionsModel
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.delete
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.post
import io.thoth.openapi.ktor.put
import org.jetbrains.exposed.sql.transactions.transaction

fun Routing.authRoutes() {
    post<Api.Auth.Login, ThothLoginUser, ThothAccessToken>(withTransaction(RouteHandler::loginUser))

    post<Api.Auth.Logout, Unit, Unit>(withTransaction(RouteHandler::logoutUser))

    post<Api.Auth.Register, ThothRegisterUser, ThothUser>(
        withTransaction(RouteHandler::registerUser),
    )

    get<Api.Auth.Jwks, JWKs>(withTransaction(RouteHandler::getJwks))

    put<
        Api.Auth.User.Id.Permissions,
        ThothModifyPermissions<UserPermissionsModel>,
        ThothUser,
        >(
        withTransaction(RouteHandler::modifyUserPermissions),
    )

    get<Api.Auth.User.Id, ThothUser>(withTransaction(RouteHandler::displayUser))

    get<Api.Auth.User.Current, ThothUser>(withTransaction(RouteHandler::currentUser))

    get<Api.Auth.User.All, List<ThothUser>>(withTransaction(RouteHandler::listUsers))

    delete<Api.Auth.User.Id, Unit, Unit>(withTransaction(RouteHandler::deleteUser))

    post<Api.Auth.User.Id.Username, ThothRenameUser, ThothUser>(
        withTransaction(RouteHandler::renameUser),
    )

    post<Api.Auth.User.Id.Password, ThothChangePassword, Unit>(withTransaction(RouteHandler::changeUserPassword))

    post<Api.Auth.User.Refresh, Unit, ThothAccessToken>(withTransaction(RouteHandler::getRefreshToken))
}

private fun <P1, P2, R> withTransaction(func: RouteHandler.(p1: P1, p2: P2) -> R): suspend RouteHandler.(P1, P2) -> R {
    return { a, b -> transaction { func(a, b) } }
}

private fun <P1, R> withTransaction(func: RouteHandler.(p1: P1) -> R): suspend RouteHandler.(P1) -> R {
    return { a -> transaction { func(a) } }
}
