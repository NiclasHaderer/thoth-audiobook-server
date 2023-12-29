package io.thoth.auth.models

interface ThothRegisterUser {
    val username: String
    val password: String
}

data class ThothRegisterUserImpl(
    override val username: String,
    override val password: String,
) : ThothRegisterUser
