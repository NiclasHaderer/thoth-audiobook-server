package io.thoth.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.auth.models.ThothRegisteredUser
import io.thoth.auth.models.ThothUserPermissions
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import java.security.KeyPair
import java.util.concurrent.TimeUnit

internal typealias KeyId = String

internal typealias GetPrincipal<ID, PERMISSIONS> =
    ApplicationCall.(jwtCredential: JWTCredential, setError: (error: JwtError) -> Unit) -> ThothPrincipal<
            ID,
            PERMISSIONS,
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

class ThothAuthConfig<ID : Any, PERMISSIONS : ThothUserPermissions>(
    val production: Boolean = true,
    val ssl: Boolean = true,
    val firstUserIsAdmin: Boolean = true,
    val realm: String? = null,
    val accessTokenExpiryTime: Long,
    val refreshTokenExpiryTime: Long,
    val keyPairs: Map<KeyId, KeyPair>,
    val activeKeyId: KeyId,
    val issuer: String,
    val domain: String,
    val protocol: URLProtocol,
    val jwksPath: String,
    val guards: Map<String, GetPrincipal<ID, PERMISSIONS>>,
    val serializePermissions: (permissions: PERMISSIONS) -> String,
    val getUserByUsername: (username: String) -> ThothDatabaseUser<ID, PERMISSIONS>?,
    val allowNewSignups: () -> Boolean,
    val getUserById: (id: ID) -> ThothDatabaseUser<ID, PERMISSIONS>?,
    val isFirstUser: () -> Boolean,
    val createUser: (registeredUser: ThothRegisteredUser) -> ThothDatabaseUser<ID, PERMISSIONS>,
    val listAllUsers: () -> List<ThothDatabaseUser<ID, PERMISSIONS>>,
    val deleteUser: (user: ThothDatabaseUser<ID, PERMISSIONS>) -> Unit,
    val renameUser: (user: ThothDatabaseUser<ID, PERMISSIONS>, newName: String) -> ThothDatabaseUser<ID, PERMISSIONS>,
    val updatePassword:
        (user: ThothDatabaseUser<ID, PERMISSIONS>, newPassword: String) -> ThothDatabaseUser<ID, PERMISSIONS>,
    val updateUserPermissions:
        (user: ThothDatabaseUser<ID, PERMISSIONS>, newPermissions: PERMISSIONS) -> ThothDatabaseUser<ID, PERMISSIONS>,
    val passwordMeetsRequirements: (password: String) -> Pair<Boolean, String?>,
    val usernameMeetsRequirements: (username: String) -> Pair<Boolean, String?>,
) {

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

    internal fun Application.applyGuards() {
        install(Authentication) {
            guards.forEach { (guard, getPrincipal) ->
                jwt(guard) {
                    if (this@ThothAuthConfig.realm != null) {
                        realm = this@ThothAuthConfig.realm
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
}

class ThothAuthConfigBuilder<ID : Any, PERMISSIONS : ThothUserPermissions> {
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

    // Functionality
    private lateinit var serializePermissions: (permissions: PERMISSIONS) -> String
    private lateinit var getUserByUsername: (username: String) -> ThothDatabaseUser<ID, PERMISSIONS>?
    private lateinit var allowNewSignups: () -> Boolean
    private lateinit var getUserById: (id: ID) -> ThothDatabaseUser<ID, PERMISSIONS>?
    private lateinit var isFirstUser: () -> Boolean
    private lateinit var createUser: (registeredUser: ThothRegisteredUser) -> ThothDatabaseUser<ID, PERMISSIONS>
    private lateinit var listAllUsers: () -> List<ThothDatabaseUser<ID, PERMISSIONS>>
    private lateinit var deleteUser: (user: ThothDatabaseUser<ID, PERMISSIONS>) -> Unit
    private lateinit var renameUser:
        (user: ThothDatabaseUser<ID, PERMISSIONS>, newName: String) -> ThothDatabaseUser<ID, PERMISSIONS>
    private lateinit var updatePassword:
        (user: ThothDatabaseUser<ID, PERMISSIONS>, newPassword: String) -> ThothDatabaseUser<ID, PERMISSIONS>
    private lateinit var updateUserPermissions:
        (user: ThothDatabaseUser<ID, PERMISSIONS>, newPermissions: PERMISSIONS) -> ThothDatabaseUser<ID, PERMISSIONS>
    private var passwordMeetsRequirements: (password: String) -> Pair<Boolean, String?> = { password ->
        if (password.length < 6) {
            Pair(false, "Password must be at least 6 characters long")
        } else {
            Pair(true, null)
        }
    }
    private var usernameMeetsRequirements: (username: String) -> Pair<Boolean, String?> = { username ->
        if (username.length < 3) {
            Pair(false, "Username must be at least 3 characters long")
        } else if (!username.matches(Regex("^[a-zA-Z0-9_-]*$"))) {
            Pair(false, "Username must be alphanumeric, including - and _")
        } else {
            Pair(true, null)
        }
    }

    // Guards
    private val guards = mutableMapOf<String, GetPrincipal<ID, PERMISSIONS>>()

    fun configureGuard(guard: String, getPrincipal: GetPrincipal<ID, PERMISSIONS>) {
        guards[guard] = getPrincipal
    }

    // Builder
    fun build(): ThothAuthConfig<ID, PERMISSIONS> {
        keyPairs.values.forEach { keyPair ->
            require(keyPair.public.algorithm == "RSA") { "KeyPair ${keyPair.public} is not a RSA key pair" }
        }
        require(keyPairs.isNotEmpty()) { "No key pairs configured" }
        require(keyPairs.containsKey(activeKeyId)) { "Active key ID '$activeKeyId' not found in key pairs" }

        return ThothAuthConfig(
            production = production,
            ssl = ssl,
            firstUserIsAdmin = firstUserIsAdmin,
            realm = realm,
            accessTokenExpiryTime = accessTokenExpiryTime,
            refreshTokenExpiryTime = refreshTokenExpiryTime,
            keyPairs = keyPairs,
            activeKeyId = activeKeyId,
            issuer = issuer,
            domain = domain,
            protocol = protocol,
            jwksPath = jwksPath,
            guards = guards.toMap(),
            serializePermissions = serializePermissions,
            getUserByUsername = getUserByUsername,
            allowNewSignups = allowNewSignups,
            getUserById = getUserById,
            isFirstUser = isFirstUser,
            createUser = createUser,
            listAllUsers = listAllUsers,
            deleteUser = deleteUser,
            renameUser = renameUser,
            updatePassword = updatePassword,
            updateUserPermissions = updateUserPermissions,
            passwordMeetsRequirements = passwordMeetsRequirements,
            usernameMeetsRequirements = usernameMeetsRequirements
        )
    }

    // Operations
    fun serializePermissions(permissions: (permissions: PERMISSIONS) -> String) {
        this.serializePermissions = permissions
    }

    fun getUserByUsername(getUserByUsername: (username: String) -> ThothDatabaseUser<ID, PERMISSIONS>?) {
        this.getUserByUsername = getUserByUsername
    }

    fun allowNewSignups(allowNewSignups: () -> Boolean) {
        this.allowNewSignups = allowNewSignups
    }

    fun getUserById(getUserById: (id: ID) -> ThothDatabaseUser<ID, PERMISSIONS>?) {
        this.getUserById = getUserById
    }

    fun isFirstUser(isFirstUser: () -> Boolean) {
        this.isFirstUser = isFirstUser
    }

    fun createUser(createUser: (registeredUser: ThothRegisteredUser) -> ThothDatabaseUser<ID, PERMISSIONS>) {
        this.createUser = createUser
    }

    fun listAllUsers(listAllUsers: () -> List<ThothDatabaseUser<ID, PERMISSIONS>>) {
        this.listAllUsers = listAllUsers
    }

    fun deleteUser(deleteUser: (user: ThothDatabaseUser<ID, PERMISSIONS>) -> Unit) {
        this.deleteUser = deleteUser
    }

    fun renameUser(
        renameUser: (user: ThothDatabaseUser<ID, PERMISSIONS>, newName: String) -> ThothDatabaseUser<ID, PERMISSIONS>
    ) {
        this.renameUser = renameUser
    }

    fun updatePassword(
        updatePassword:
            (user: ThothDatabaseUser<ID, PERMISSIONS>, newPassword: String) -> ThothDatabaseUser<ID, PERMISSIONS>
    ) {
        this.updatePassword = updatePassword
    }

    fun updateUserPermissions(
        updateUserPermissions:
            (user: ThothDatabaseUser<ID, PERMISSIONS>, newPermissions: PERMISSIONS) -> ThothDatabaseUser<
                    ID, PERMISSIONS
                >
    ) {
        this.updateUserPermissions = updateUserPermissions
    }

    fun passwordMeetsRequirements(passwordMeetsRequirements: (password: String) -> Pair<Boolean, String?>) {
        this.passwordMeetsRequirements = passwordMeetsRequirements
    }

    fun usernameMeetsRequirements(usernameMeetsRequirements: (username: String) -> Pair<Boolean, String?>) {
        this.usernameMeetsRequirements = usernameMeetsRequirements
    }
}
