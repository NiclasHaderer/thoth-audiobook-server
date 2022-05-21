package io.thoth.auth.routes

import io.ktor.application.*
import io.ktor.routing.*
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
            route("user/{userID}") {
                modifyUser()
            }
        }

        userAuth {
            route("user") {
                userEndpoint()

                route("password") {
                    changePassword()
                }
            }
        }
    }
}
