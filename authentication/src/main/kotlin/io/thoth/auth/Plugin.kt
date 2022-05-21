package io.thoth.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.response.*
import io.thoth.auth.routes.authRoutes
import java.security.KeyPair
import java.util.concurrent.TimeUnit


class AuthConfig(
    val keyPair: KeyPair,
    val keyId: String,
    val issuer: String,
    val realm: String? = null
)


fun Application.authentication(config: AuthConfig) {

    val jwkProvider = JwkProviderBuilder(config.issuer)
        .cached(5, 10, TimeUnit.MINUTES)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt(GuardTypes.User.value) {
            if (config.realm != null) {
                realm = config.realm
            }

            verifier(jwkProvider, config.issuer) {
                acceptLeeway(3)
            }
            validate { jwtCredential ->
                val principal = jwtToPrincipal(jwtCredential) ?: return@validate null
                if (principal.type == JwtType.Access) principal else null
            }
            challenge { _, _ ->
                println(jwkProvider.get(config.keyId))
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }

        jwt(GuardTypes.EditUser.value) {
            if (config.realm != null) {
                realm = config.realm
            }
            verifier(jwkProvider, config.issuer) {
                acceptLeeway(3)
            }
            validate { jwtCredential ->
                val principal = jwtToPrincipal(jwtCredential) ?: return@validate null
                if (principal.type != JwtType.Access && principal.edit) principal else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }

        jwt(GuardTypes.AdminUser.value) {
            if (config.realm != null) {
                realm = config.realm
            }
            verifier(jwkProvider, config.issuer) {
                acceptLeeway(3)
            }
            validate { jwtCredential ->
                val principal = jwtToPrincipal(jwtCredential) ?: return@validate null
                if (principal.type != JwtType.Access && principal.edit && principal.admin) principal else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }
    }

    authRoutes(config)
}

