package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.auth.JwtPair
import io.thoth.auth.routes.AuthRoutes
import io.thoth.auth.routes.JwksResponse
import io.thoth.auth.routes.LoginUser
import io.thoth.auth.routes.ModifyUser
import io.thoth.auth.routes.PasswordChange
import io.thoth.auth.routes.RegisterUser
import io.thoth.auth.routes.UsernameChange
import io.thoth.models.UserModel
import io.thoth.openapi.routing.delete
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.post
import io.thoth.openapi.routing.put

fun Routing.authRoutes(routes: AuthRoutes) {
    post<Api.Auth.Login, LoginUser, JwtPair> { _, loginUser -> routes.login.invoke(this, loginUser) }
    post<Api.Auth.Register, RegisterUser, UserModel> { _, registerUser -> routes.register.invoke(this, registerUser) }
    get<Api.Auth.Jwks, JwksResponse> { routes.jwks.invoke(this) }
    put<Api.Auth.User.Id, ModifyUser, UserModel> { route, editUser -> routes.modify.invoke(this, route.id, editUser) }
    get<Api.Auth.User, UserModel> { routes.getUser.invoke(this) }
    delete<Api.Auth.User, Unit> { routes.delete.invoke(this) }
    post<Api.Auth.User.Username, UsernameChange, UserModel> { _, usernameChange ->
        routes.changeUsername.invoke(this, usernameChange)
    }
    post<Api.Auth.User.Password, PasswordChange, Unit> { _, passwordChange ->
        routes.changePassword.invoke(this, passwordChange)
    }
}
