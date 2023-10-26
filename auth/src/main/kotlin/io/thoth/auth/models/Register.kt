package io.thoth.auth.models

interface RegisteredUser {
    val username: String
    val passwordHash: String
    val admin: Boolean
}

internal data class RegisteredUserImpl(
    override val username: String,
    override val passwordHash: String,
    override val admin: Boolean,
) : RegisteredUser

interface ThothRegisterUser {
    val username: String
    val password: String
}
