package io.thoth.auth.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.thoth.auth.AuthConfig
import io.thoth.auth.adminUserAuth
import io.thoth.auth.userAuth

internal fun Application.authRoutes(config: AuthConfig) {
    // TODO add ability to logout everywhere
    // TODO logout user if password gets changed

    routing {
        route("login") {
            loginEndpoint(config)
        }

        route("register") {
            registerEndpoint()
        }

        route(".well-known/jwks.json") {
            jwksEndpoint(config)
        }

        adminUserAuth {
            route("user") {
                modifyUser()
            }
        }

        userAuth {
            route("user") {
                getUser()
                changeUsername()

                route("password") {
                    changePassword()
                }
            }
        }
    }
}
