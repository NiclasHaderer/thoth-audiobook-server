package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
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
import io.thoth.auth.models.ThothUserWithPermissions
import io.thoth.models.UpdatePermissionsModel
import io.thoth.models.UserPermissionsModel
import io.thoth.openapi.ktor.delete
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.post
import io.thoth.openapi.ktor.put
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Routing.authRoutes() {
    post<Api.Auth.Login, ThothLoginUser, ThothAccessToken>(withTransaction(RoutingContext::loginUser))

    post<Api.Auth.Logout, Unit, Unit>(withTransaction(RoutingContext::logoutUser))

    post<Api.Auth.Register, ThothRegisterUser, ThothUser>(withTransaction(RoutingContext::registerUser))

    get<Api.Auth.Jwks, JWKs>(withTransaction(RoutingContext::getJwks))

    put<Api.Auth.User.Id.Permissions, ThothModifyPermissions<UpdatePermissionsModel>, ThothUser>(
        withTransaction(RoutingContext::modifyUserPermissions),
    )

    get<Api.Auth.User.Id, ThothUser>(withTransaction(RoutingContext::displayUser))

    get<Api.Auth.User.Current, ThothUserWithPermissions<UserPermissionsModel>>(
        withTransaction(RoutingContext::currentUser),
    )

    get<Api.Auth.User.All, List<ThothUserWithPermissions<UserPermissionsModel>>>(
        withTransaction(RoutingContext::listUsers),
    )

    delete<Api.Auth.User.Id, Unit, Unit>(withTransaction(RoutingContext::deleteUser))

    post<Api.Auth.User.Id.Username, ThothRenameUser, ThothUser>(withTransaction(RoutingContext::renameUser))

    post<Api.Auth.User.Id.Password, ThothChangePassword, Unit>(withTransaction(RoutingContext::changeUserPassword))

    post<Api.Auth.User.Refresh, Unit, ThothAccessToken>(withTransaction(RoutingContext::getRefreshToken))
}

private fun <P1, P2, R> withTransaction(
    func: RoutingContext.(p1: P1, p2: P2) -> R,
): suspend RoutingContext.(P1, P2) -> R =
    { a, b ->
        transaction {
            func(a, b)
        }
    }

private fun <P1, R> withTransaction(func: RoutingContext.(p1: P1) -> R): suspend RoutingContext.(P1) -> R =
    { a ->
        transaction {
            func(a)
        }
    }
