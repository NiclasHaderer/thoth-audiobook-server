package io.thoth.auth.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.thoth.auth.AuthConfig
import io.thoth.auth.adminUserAuth
import io.thoth.auth.userAuth

internal fun Application.authRoutes(config: AuthConfig) {
    routing {
        route("login") {
            loginEndpoint(config)
        }

        route("register") {
            registerEndpoint(config)
        }

        route(".well-known/jwks.json") {
            jwksEndpoint(config)
        }

        adminUserAuth {
            // TODO use the location api in order to ensure that the userID is a UUID before it gets parsed anywhere else
            route("user/{userID}") {
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
