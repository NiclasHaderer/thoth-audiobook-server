package io.thoth.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.util.*
import io.thoth.auth.models.RegisteredUserImpl
import io.thoth.auth.models.ThothDatabaseUser
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

class ThothAuthConfig {
    var production = true

    // Default user settings
    var firstUserIsAdmin = true
    var includePermissionsInJwt = true

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
