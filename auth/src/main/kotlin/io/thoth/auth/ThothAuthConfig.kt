package io.thoth.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.thoth.auth.models.RegisteredUserImpl
import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import java.security.KeyPair
import java.util.concurrent.TimeUnit

typealias KeyId = String

val PLUGIN_CONFIG_KEY = AttributeKey<ThothAuthConfig>("ThothAuthPlugin")

fun RouteHandler.thothAuthConfig(): ThothAuthConfig {
    if (!call.attributes.contains(PLUGIN_CONFIG_KEY)) {
        throw IllegalStateException("ThothAuthPlugin not installed")
    }

    return call.attributes[PLUGIN_CONFIG_KEY]
}

data class JwtError(val error: String, val statusCode: HttpStatusCode)

val JWT_VALIDATION_FAILED = AttributeKey<JwtError>("JWT_VALIDATION_FAILED")

class ThothAuthConfig {
    var production = true

    // Default user settings
    var firstUserIsAdmin = true
    var includePermissionsInJwt = true
    var realm: String? = null

    // Token expiry times
    var accessTokenExpiryTime = TimeUnit.MINUTES.toMillis(5)
    var refreshTokenExpiryTime = TimeUnit.DAYS.toMillis(60)

    // Key pairs
    val keyPairs: Map<KeyId, KeyPair> = mapOf()
    lateinit var activeKeyId: KeyId

    // Application paths
    lateinit var issuer: String
    lateinit var domain: String
    lateinit var protocol: URLProtocol
    lateinit var jwksPath: String

    // TODO make sure that the KeyPair is a RSA key pair
    internal fun assertAreRsaKeyPairs() {
        keyPairs.values.forEach { keyPair ->
            require(keyPair.public.algorithm == "RSA") { "KeyPair ${keyPair.public} is not a RSA key pair" }
        }
    }

    internal val jwkProvider by lazy {
        val url =
            URLBuilder()
                .apply {
                    protocol = this@ThothAuthConfig.protocol
                    host = this@ThothAuthConfig.domain
                    encodedPath = this@ThothAuthConfig.jwksPath
                }
                .build()
                .toURI()
                .toURL()
        JwkProviderBuilder(url).cached(10, 24, TimeUnit.HOURS).rateLimited(10, 1, TimeUnit.MINUTES).build()
    }

    fun AuthenticationConfig.configureGuard(
        guard: ThothAuthGuard,
        getPrincipal:
            ApplicationCall.(jwtCredential: JWTCredential, setError: (error: JwtError) -> Unit) -> ThothPrincipal?
    ) {
        jwt(guard.name()) {
            if (this@ThothAuthConfig.realm != null) {
                realm = this@ThothAuthConfig.realm!!
            }

            verifier(jwkProvider, this@ThothAuthConfig.issuer) { acceptLeeway(3) }

            validate { jwtCredential ->
                val principal =
                    getPrincipal(jwtCredential) { error -> attributes.put(JWT_VALIDATION_FAILED, error) }
                        ?: return@validate null

                if (principal.type != ThothJwtTypes.Access) {
                    attributes.put(
                        JWT_VALIDATION_FAILED,
                        JwtError("JWT is not an access token", HttpStatusCode.Unauthorized),
                    )
                    return@validate null
                }

                attributes.put(
                    JWT_VALIDATION_FAILED,
                    JwtError("JWT is not an access token", HttpStatusCode.Unauthorized),
                )

                return@validate principal
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

    // Operations
    fun getUserByUsername(username: String): ThothDatabaseUser? {
        TODO("Operation not implemented by application")
    }

    fun <T : Any> getUserById(id: T): ThothDatabaseUser? {
        TODO("Operation not implemented by application")
    }

    fun isFirstUser(): Boolean {
        TODO("Operation not implemented by application")
    }

    fun createUser(registeredUserImpl: RegisteredUserImpl): ThothDatabaseUser {
        TODO("Operation not implemented by application")
    }

    fun listAllUsers(): List<ThothDatabaseUser> {
        TODO("Operation not implemented by application")
    }

    fun deleteUser(user: ThothDatabaseUser) {
        TODO("Operation not implemented by application")
    }

    fun <T : Any> renameUser(user: ThothDatabaseUser, id: T): ThothDatabaseUser {
        TODO("Operation not implemented by application")
    }

    fun updatePassword(user: ThothDatabaseUser, newPassword: String): ThothDatabaseUser {
        TODO("Operation not implemented by application")
    }

    fun updateUserPermissions(
        user: ThothDatabaseUser,
        permissions: Map<String, Any>,
        isAdmin: Boolean
    ): ThothDatabaseUser {
        TODO("Operation not implemented by application")
    }
}
