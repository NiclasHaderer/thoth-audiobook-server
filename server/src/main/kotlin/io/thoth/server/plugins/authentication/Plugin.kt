package io.thoth.server.plugins.authentication

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.util.concurrent.TimeUnit
import org.koin.ktor.ext.inject

fun Application.configureAuthentication() {
    val authConfig by inject<AuthConfig>()

    val url =
        URLBuilder()
            .apply {
                protocol = authConfig.protocol
                host = authConfig.domain
                encodedPath = authConfig.jwksPath
            }
            .build()
            .toURI()
            .toURL()

    val jwkProvider =
        JwkProviderBuilder(url).cached(10, 24, TimeUnit.HOURS).rateLimited(10, 1, TimeUnit.MINUTES).build()

    install(Authentication) {
        jwt(Guards.Normal) {
            if (authConfig.realm != null) {
                realm = authConfig.realm!!
            }

            verifier(jwkProvider, authConfig.issuer) { acceptLeeway(3) }

            validate { jwtCredential ->
                val principal = jwtToPrincipal(jwtCredential) ?: return@validate null
                if (principal.type == JwtType.Access) principal else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }

        jwt(Guards.Editor) {
            if (authConfig.realm != null) {
                realm = authConfig.realm!!
            }

            verifier(jwkProvider, authConfig.issuer) { acceptLeeway(3) }

            validate { jwtCredential ->
                val principal = jwtToPrincipal(jwtCredential) ?: return@validate null
                if (principal.type == JwtType.Access && principal.edit) principal else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }

        jwt(Guards.Admin) {
            if (authConfig.realm != null) {
                realm = authConfig.realm!!
            }

            verifier(jwkProvider, authConfig.issuer) { acceptLeeway(3) }

            validate { jwtCredential ->
                val principal = jwtToPrincipal(jwtCredential) ?: return@validate null
                if (principal.type == JwtType.Access && principal.edit && principal.admin) principal else null
            }
            challenge { a, b ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }
    }
}
