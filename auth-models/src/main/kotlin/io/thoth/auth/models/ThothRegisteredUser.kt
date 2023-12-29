package io.thoth.auth.models

interface ThothRegisteredUser {
    val username: String
    val passwordHash: String
    val admin: Boolean
}

data class ThothRegisteredUserImpl(
    override val username: String,
    override val passwordHash: String,
    override val admin: Boolean,
) : ThothRegisteredUser


