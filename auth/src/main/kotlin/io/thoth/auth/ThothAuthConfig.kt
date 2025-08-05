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
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import java.security.KeyPair
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

internal typealias KeyId = String

internal typealias GetPrincipal =
    ApplicationCall.(jwtCredential: JWTCredential, setError: (error: JwtError) -> Unit) -> ThothPrincipal?

internal val PLUGIN_CONFIG_KEY = AttributeKey<ThothAuthConfig<*>>("ThothAuthPlugin")

internal fun <PERMISSIONS> RouteHandler.thothAuthConfig(): ThothAuthConfig<PERMISSIONS> {
    if (!call.attributes.contains(PLUGIN_CONFIG_KEY)) {
        throw IllegalStateException("ThothAuthPlugin not installed")
    }

    @Suppress("UNCHECKED_CAST")
    return call.attributes[PLUGIN_CONFIG_KEY] as ThothAuthConfig<PERMISSIONS>
}

data class JwtError(val error: String, val statusCode: HttpStatusCode)

private val JWT_VALIDATION_FAILED = AttributeKey<JwtError>("JWT_VALIDATION_FAILED")

class ThothAuthConfig<PERMISSIONS>(
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
    val port: Int,
    val protocol: URLProtocol,
    val jwksPath: String,
    val guards: Map<String, GetPrincipal>,
    val getUserByUsername: (username: String) -> ThothDatabaseUser?,
    val allowNewSignups: () -> Boolean,
    val getUserById: (id: UUID) -> ThothDatabaseUser?,
    val isFirstUser: () -> Boolean,
    val createUser: (registeredUser: ThothRegisteredUser) -> ThothDatabaseUser,
    val listAllUsers: () -> List<ThothDatabaseUser>,
    val deleteUser: (user: ThothDatabaseUser) -> Unit,
    val renameUser: (user: ThothDatabaseUser, newName: String) -> ThothDatabaseUser,
    val updatePassword: (user: ThothDatabaseUser, newPassword: String) -> ThothDatabaseUser,
    val updateUserPermissions: (user: ThothDatabaseUser, newPermissions: PERMISSIONS) -> ThothDatabaseUser,
    val passwordMeetsRequirements: (password: String) -> Pair<Boolean, String?>,
    val usernameMeetsRequirements: (username: String) -> Pair<Boolean, String?>,
    private val isAdminUser: (user: ThothDatabaseUser) -> Boolean,
) {

    internal val jwkProvider by lazy {
        val url =
            URLBuilder()
                .apply {
                    protocol = this@ThothAuthConfig.protocol
                    host = this@ThothAuthConfig.domain
                    port = this@ThothAuthConfig.port
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

    internal fun isAdmin(principal: ThothPrincipal): Boolean {
        return isAdminUser(getUserById(principal.userId) ?: return false)
    }
}

class ThothAuthConfigBuilder<PERMISSIONS> {
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
    var port by Delegates.notNull<Int>()
    lateinit var protocol: URLProtocol
    lateinit var jwksPath: String

    // Functionality
    private lateinit var getUserByUsername: (username: String) -> ThothDatabaseUser?
    private lateinit var allowNewSignups: () -> Boolean
    private lateinit var getUserById: (id: UUID) -> ThothDatabaseUser?
    private lateinit var isFirstUser: () -> Boolean
    private lateinit var createUser: (registeredUser: ThothRegisteredUser) -> ThothDatabaseUser
    private lateinit var listAllUsers: () -> List<ThothDatabaseUser>
    private lateinit var deleteUser: (user: ThothDatabaseUser) -> Unit
    private lateinit var renameUser: (user: ThothDatabaseUser, newName: String) -> ThothDatabaseUser
    private lateinit var updatePassword: (user: ThothDatabaseUser, newPassword: String) -> ThothDatabaseUser
    private lateinit var updateUserPermissions:
        (user: ThothDatabaseUser, newPermissions: PERMISSIONS) -> ThothDatabaseUser
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
    private lateinit var isAdminUser: (user: ThothDatabaseUser) -> Boolean

    // Guards
    private val guards = mutableMapOf<String, GetPrincipal>()

    fun configureGuard(guard: String, getPrincipal: GetPrincipal) {
        guards[guard] = getPrincipal
    }

    // Builder
    fun build(): ThothAuthConfig<PERMISSIONS> {
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
            port = port,
            protocol = protocol,
            jwksPath = jwksPath,
            guards = guards.toMap(),
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
            usernameMeetsRequirements = usernameMeetsRequirements,
            isAdminUser = isAdminUser,
        )
    }

    // Operations
    fun getUserByUsername(getUserByUsername: (username: String) -> ThothDatabaseUser?) {
        this.getUserByUsername = getUserByUsername
    }

    fun allowNewSignups(allowNewSignups: () -> Boolean) {
        this.allowNewSignups = allowNewSignups
    }

    fun isAdminUser(isAdminUser: (user: ThothDatabaseUser) -> Boolean) {
        this.isAdminUser = isAdminUser
    }

    fun getUserById(getUserById: (id: UUID) -> ThothDatabaseUser?) {
        this.getUserById = getUserById
    }

    fun isFirstUser(isFirstUser: () -> Boolean) {
        this.isFirstUser = isFirstUser
    }

    fun createUser(createUser: (registeredUser: ThothRegisteredUser) -> ThothDatabaseUser) {
        this.createUser = createUser
    }

    fun listAllUsers(listAllUsers: () -> List<ThothDatabaseUser>) {
        this.listAllUsers = listAllUsers
    }

    fun deleteUser(deleteUser: (user: ThothDatabaseUser) -> Unit) {
        this.deleteUser = deleteUser
    }

    fun renameUser(renameUser: (user: ThothDatabaseUser, newName: String) -> ThothDatabaseUser) {
        this.renameUser = renameUser
    }

    fun updatePassword(updatePassword: (user: ThothDatabaseUser, newPassword: String) -> ThothDatabaseUser) {
        this.updatePassword = updatePassword
    }

    fun updateUserPermissions(
        updateUserPermissions: (user: ThothDatabaseUser, newPermissions: PERMISSIONS) -> ThothDatabaseUser
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
