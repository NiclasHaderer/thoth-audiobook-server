package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.generators.openapi.delete
import io.thoth.generators.openapi.get
import io.thoth.generators.openapi.post
import io.thoth.generators.openapi.put
import io.thoth.models.UserModel
import io.thoth.server.authentication.JwtPair
import io.thoth.server.authentication.routes.AuthRoutes
import io.thoth.server.authentication.routes.JWKsResponse
import io.thoth.server.authentication.routes.LoginUser
import io.thoth.server.authentication.routes.ModifyUser
import io.thoth.server.authentication.routes.PasswordChange
import io.thoth.server.authentication.routes.RegisterUser
import io.thoth.server.authentication.routes.UsernameChange

fun Routing.authRoutes(routes: AuthRoutes) {
    post<Api.Auth.Login, LoginUser, JwtPair> { _, loginUser -> routes.login.invoke(this, loginUser) }
    post<Api.Auth.Register, RegisterUser, UserModel> { _, registerUser -> routes.register.invoke(this, registerUser) }
    get<Api.Auth.Jwks, JWKsResponse> { routes.jwks.invoke(this) }
    put<Api.Auth.User.Id, ModifyUser, UserModel> { route, editUser -> routes.modify.invoke(this, route.id, editUser) }
    get<Api.Auth.User, UserModel> { routes.getUser.invoke(this) }
    delete<Api.Auth.User, Unit, Unit> { _, _,
        ->
        routes.delete.invoke(this)
    }
    post<Api.Auth.User.Username, UsernameChange, UserModel> { _, usernameChange ->
        routes.changeUsername.invoke(this, usernameChange)
    }
    post<Api.Auth.User.Password, PasswordChange, Unit> { _, passwordChange ->
        routes.changePassword.invoke(this, passwordChange)
    }
}
