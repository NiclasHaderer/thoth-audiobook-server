package io.thoth.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import java.security.KeyPair
import java.util.concurrent.TimeUnit

class AuthConfig(
    val keyPair: KeyPair,
    val issuer: String,
    val realm: String? = null
)


fun Application.authorization(config: AuthConfig) {

    val jwkProvider = JwkProviderBuilder(config.issuer)
        .cached(5, 10, TimeUnit.MINUTES)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        // TODO do not seem to do anything at the moment
        jwt("user-jwt") {
            if (config.realm != null) {
                realm = config.realm
            }
            verifier(jwkProvider, config.issuer) {
                acceptLeeway(3)
            }
            validate { jwtCredential ->
                val principal = credentialsToPrincipal(jwtCredential) ?: return@validate null
                if (principal.type == JwtType.access) principal else null
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }
        jwt("edit-user-jwt") {
            if (config.realm != null) {
                realm = config.realm
            }
            verifier(jwkProvider, config.issuer) {
                acceptLeeway(3)
            }
            validate { jwtCredential ->
                val principal = credentialsToPrincipal(jwtCredential) ?: return@validate null
                if (principal.type != JwtType.access && principal.edit) principal else null
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }
        jwt("admin-user-jwt") {
            if (config.realm != null) {
                realm = config.realm
            }
            verifier(jwkProvider, config.issuer) {
                acceptLeeway(3)
            }
            validate { jwtCredential ->
                val principal = credentialsToPrincipal(jwtCredential) ?: return@validate null
                if (principal.type != JwtType.access && principal.edit && principal.admin) principal else null
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }
    }


    routing {

        route("login") {
            loginEndpoint(config)
        }
        adminUserAuth {
            route("register") {
                registerEndpoint(config)
            }
        }
        route(".well-known/jwks.json") {
            jwksEndpoint(config.keyPair)
        }
        userAuth {
            route("user") {
                userEndpoint()
            }
        }
    }
}

