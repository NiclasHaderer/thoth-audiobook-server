package io.thoth.auth.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.thoth.auth.AuthConfig
import io.thoth.auth.adminUserAuth
import io.thoth.auth.userAuth
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.routing.post

internal fun Application.authRoutes(config: AuthConfig) {

    routing {
        route("login") {
            post(login(config))
        }

        route("register") {
            post(RouteHandler::register)
        }

        route(".well-known/jwks.json") {
            get(jwksEndpoint(config))
        }

        adminUserAuth {
            route("user") {
                patch(RouteHandler::modifyUser)
            }
        }

        userAuth {
            route("user") {

                // TODO add ability to logout everywhere
                get(RouteHandler::getUser)
                post(RouteHandler::changeUsername)

                route("password") {
                    // TODO logout user if password gets changed
                    post(RouteHandler::changePassword)
                }
            }
        }
    }
}
