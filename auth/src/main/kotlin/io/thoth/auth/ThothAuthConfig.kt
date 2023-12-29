package io.thoth.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.thoth.auth.models.ThothRegisteredUser
import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.auth.models.ThothUserPermissions
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import java.security.KeyPair
import java.util.concurrent.TimeUnit

internal typealias KeyId = String

internal typealias GetPrincipal<ID, PERMISSIONS> =
    ApplicationCall.(jwtCredential: JWTCredential, setError: (error: JwtError) -> Unit) -> ThothPrincipal<
            ID, PERMISSIONS
        >?

internal val PLUGIN_CONFIG_KEY = AttributeKey<ThothAuthConfig<*, *>>("ThothAuthPlugin")

internal fun <ID : Any, PERMISSIONS : ThothUserPermissions> RouteHandler.thothAuthConfig():
    ThothAuthConfig<ID, PERMISSIONS> {
    if (!call.attributes.contains(PLUGIN_CONFIG_KEY)) {
        throw IllegalStateException("ThothAuthPlugin not installed")
    }

    @Suppress("UNCHECKED_CAST") return call.attributes[PLUGIN_CONFIG_KEY] as ThothAuthConfig<ID, PERMISSIONS>
}

data class JwtError(val error: String, val statusCode: HttpStatusCode)

private val JWT_VALIDATION_FAILED = AttributeKey<JwtError>("JWT_VALIDATION_FAILED")

class ThothAuthConfig<ID : Any, PERMISSIONS : ThothUserPermissions> {
    var production = true
    var ssl = true

    // Default user settings
    var firstUserIsAdmin = true
    var realm: String? = null

    // Token expiry times
    var accessTokenExpiryTime = TimeUnit.MINUTES.toMillis(5)
    var refreshTokenExpiryTime = TimeUnit.DAYS.toMillis(60)

    // Key pairs
    val keyPairs: MutableMap<KeyId, KeyPair> = mutableMapOf()
    lateinit var activeKeyId: KeyId

    // Application paths
    lateinit var issuer: String
    lateinit var domain: String
    lateinit var protocol: URLProtocol
    lateinit var jwksPath: String

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

    private val guards = mutableMapOf<String, GetPrincipal<ID, PERMISSIONS>>()

    internal fun assertAreRsaKeyPairs() {
        keyPairs.values.forEach { keyPair ->
            require(keyPair.public.algorithm == "RSA") { "KeyPair ${keyPair.public} is not a RSA key pair" }
        }
        require(keyPairs.isNotEmpty()) { "No key pairs configured" }
        require(keyPairs.containsKey(activeKeyId)) { "Active key ID '$activeKeyId' not found in key pairs" }
    }

    fun configureGuard(guard: String, getPrincipal: GetPrincipal<ID, PERMISSIONS>) {
        guards[guard] = getPrincipal
    }

    internal fun Application.applyGuards() {
        install(Authentication) {
            guards.forEach { (guard, getPrincipal) ->
                jwt(guard) {
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
        }
    }

    // Operations
    var serializePermissions: (permissions: PERMISSIONS) -> String = { _ ->
        throw RuntimeException("Operation not implemented by application")
    }

    var getUserByUsername: (username: String) -> ThothDatabaseUser<ID, PERMISSIONS>? = { _ ->
        throw RuntimeException("Operation not implemented by application")
    }

    var allowNewSignups: () -> Boolean = { throw RuntimeException("Operation not implemented by application") }

    var getUserById: (id: Any) -> ThothDatabaseUser<ID, PERMISSIONS>? = { _ ->
        throw RuntimeException("Operation not implemented by application")
    }

    var isFirstUser: () -> Boolean = { throw RuntimeException("Operation not implemented by application") }

    var createUser: (registeredUser: ThothRegisteredUser) -> ThothDatabaseUser<ID, PERMISSIONS> = { _ ->
        throw RuntimeException("Operation not implemented by application")
    }

    var listAllUsers: () -> List<ThothDatabaseUser<ID, PERMISSIONS>> = {
        throw RuntimeException("Operation not implemented by application")
    }

    var deleteUser: (user: ThothDatabaseUser<ID, PERMISSIONS>) -> Unit = { _ ->
        throw RuntimeException("Operation not implemented by application")
    }

    var renameUser: (user: ThothDatabaseUser<ID, PERMISSIONS>, name: String) -> ThothDatabaseUser<ID, PERMISSIONS> =
        { _, _ ->
            throw RuntimeException("Operation not implemented by application")
        }

    var updatePassword:
        (user: ThothDatabaseUser<ID, PERMISSIONS>, newPassword: String) -> ThothDatabaseUser<ID, PERMISSIONS> =
        { _, _ ->
            throw RuntimeException("Operation not implemented by application")
        }

    var updateUserPermissions:
        (user: ThothDatabaseUser<ID, PERMISSIONS>, permissions: ThothUserPermissions) -> ThothDatabaseUser<
                ID,
                PERMISSIONS,
            > =
        { _, _ ->
            throw RuntimeException("Operation not implemented by application")
        }

    var passwordMeetsRequirements: (password: String) -> Pair<Boolean, String?> = { password ->
        if (password.length < 8) {
            Pair(false, "Password must be at least 8 characters long")
        } else {
            Pair(true, null)
        }
    }

    var usernameMeetsRequirements: (username: String) -> Pair<Boolean, String?> = { username ->
        if (username.length < 5) {
            Pair(false, "Username must be at least 5 characters long")
        } else if (!username.matches(Regex("^[a-zA-Z0-9_-]*$"))) {
            Pair(false, "Username must be alphanumeric, including - and _")
        } else {
            Pair(true, null)
        }
    }
}
