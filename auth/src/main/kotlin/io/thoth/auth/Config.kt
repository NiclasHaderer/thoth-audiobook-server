package io.thoth.auth

import java.security.KeyPair
import java.util.concurrent.TimeUnit

typealias KeyId = String

object AuthConfig {

    var accessTokenExpiryTime = TimeUnit.MINUTES.toMillis(5)
    var refreshTokenExpiryTime = TimeUnit.DAYS.toMillis(60)

    var production: Boolean = false
    lateinit var keyPair: Map<KeyId, KeyPair>
    lateinit var issuer: String
    lateinit var activeKeyId: KeyId

    // TODO make sure that the KeyPair is a RSA key pair

    fun getUserByUsername(username: String): ThothDatabaseUser? {
        TODO("Has to be implemented by the application")
    }
}
