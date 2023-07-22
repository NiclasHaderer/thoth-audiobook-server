package io.thoth.server.plugins.authentication

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.util.*
import java.util.concurrent.TimeUnit
import org.koin.ktor.ext.inject

data class JwtError(val error: String, val statusCode: HttpStatusCode)

val JWT_VALIDATION_FAILED = AttributeKey<JwtError>("JWT_VALIDATION_FAILED")

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
                val principal =
                    jwtToPrincipal(jwtCredential)
                        ?: return@validate run {
                            attributes.put(
                                JWT_VALIDATION_FAILED,
                                JwtError("JWT is not valid", HttpStatusCode.Unauthorized)
                            )
                            null
                        }
                if (principal.type == JwtType.Access) principal
                else {
                    attributes.put(
                        JWT_VALIDATION_FAILED,
                        JwtError("JWT is not an access token", HttpStatusCode.Unauthorized),
                    )
                    null
                }
            }
            challenge { _, _ ->
                val error = call.attributes.getOrNull(JWT_VALIDATION_FAILED)
                call.respond(
                    error?.statusCode ?: HttpStatusCode.Unauthorized,
                    mapOf("error" to (error?.error ?: "Unknown JWT error")),
                )
            }
        }

        jwt(Guards.Editor) {
            if (authConfig.realm != null) {
                realm = authConfig.realm!!
            }

            verifier(jwkProvider, authConfig.issuer) { acceptLeeway(3) }

            validate { jwtCredential ->
                val principal =
                    jwtToPrincipal(jwtCredential)
                        ?: return@validate run {
                            attributes.put(
                                JWT_VALIDATION_FAILED,
                                JwtError("JWT is not valid", HttpStatusCode.Unauthorized)
                            )
                            null
                        }
                if (principal.type != JwtType.Access) {
                    attributes.put(
                        JWT_VALIDATION_FAILED,
                        JwtError("JWT is not an access token", HttpStatusCode.Unauthorized),
                    )
                    return@validate null
                }

                if (principal.edit) principal
                else {
                    attributes.put(
                        JWT_VALIDATION_FAILED,
                        JwtError("JWT is not an editor token", HttpStatusCode.Forbidden),
                    )
                    null
                }
            }
            challenge { _, _ ->
                val error = call.attributes.getOrNull(JWT_VALIDATION_FAILED)
                call.respond(
                    error?.statusCode ?: HttpStatusCode.Unauthorized,
                    mapOf("error" to (error?.error ?: "Unknown JWT error")),
                )
            }
        }

        jwt(Guards.Admin) {
            if (authConfig.realm != null) {
                realm = authConfig.realm!!
            }

            verifier(jwkProvider, authConfig.issuer) { acceptLeeway(3) }

            validate { jwtCredential ->
                val principal =
                    jwtToPrincipal(jwtCredential)
                        ?: return@validate run {
                            attributes.put(
                                JWT_VALIDATION_FAILED,
                                JwtError("JWT is not valid", HttpStatusCode.Unauthorized)
                            )
                            null
                        }
                if (principal.type != JwtType.Access) {
                    attributes.put(
                        JWT_VALIDATION_FAILED,
                        JwtError("JWT is not an access token", HttpStatusCode.Unauthorized),
                    )
                    return@validate null
                }

                if (principal.admin && principal.edit) principal
                else {
                    attributes.put(
                        JWT_VALIDATION_FAILED,
                        JwtError("JWT is not an admin token", HttpStatusCode.Forbidden),
                    )
                    null
                }
            }
            challenge { _, _ ->
                val error = call.attributes.getOrNull(JWT_VALIDATION_FAILED)
                call.respond(
                    error?.statusCode ?: HttpStatusCode.Unauthorized,
                    mapOf("error" to (error?.error ?: "Unknown JWT error")),
                )
            }
        }
    }
}
