package io.thoth.auth

import io.ktor.http.*
import io.thoth.auth.models.RegisteredUserImpl
import io.thoth.auth.models.ThothDatabaseUser
import java.security.KeyPair
import java.util.concurrent.TimeUnit

typealias KeyId = String

object ThothAuthConfig {

    val accessTokenExpiryTime = TimeUnit.MINUTES.toMillis(5)
    val production = true
    val firstUserIsAdmin = true
    val refreshTokenExpiryTime = TimeUnit.DAYS.toMillis(60)
    val includePermissionsInJwt = true

    lateinit var keyPairs: Map<KeyId, KeyPair>
    lateinit var issuer: String
    lateinit var activeKeyId: KeyId
    lateinit var domain: String
    lateinit var protocol: URLProtocol
    lateinit var jwksPath: String

    // TODO make sure that the KeyPair is a RSA key pair

    fun getUserByUsername(username: String): ThothDatabaseUser? {
        TODO("Has to be implemented by the application")
    }

    fun <T : Any> getUserById(id: T): ThothDatabaseUser? {
        TODO("Has to be implemented by the application")
    }

    fun isFirstUser(): Boolean {
        TODO("Has to be implemented by the application")
    }

    fun createUser(registeredUserImpl: RegisteredUserImpl): ThothDatabaseUser {
        TODO("Has to be implemented by the application")
    }

    fun listAllUsers(): List<ThothDatabaseUser> {
        TODO("Has to be implemented by the application")
    }

    fun deleteUser(user: ThothDatabaseUser) {
        TODO("Has to be implemented by the application")
    }

    fun <T : Any> renameUser(user: ThothDatabaseUser, id: T): ThothDatabaseUser {
        TODO("Has to be implemented by the application")
    }

    fun updatePassword(user: ThothDatabaseUser, newPassword: String): ThothDatabaseUser {
        TODO("Has to be implemented by the application")
    }

    fun updateUserPermissions(
        user: ThothDatabaseUser,
        permissions: Map<String, Any>,
        isAdmin: Boolean
    ): ThothDatabaseUser {
        TODO("Has to be implemented by the application")
    }
}
