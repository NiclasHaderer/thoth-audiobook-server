package io.thoth.auth.models

interface RegisteredUser {
    val username: String
    val passwordHash: String
    val admin: Boolean
}

interface ThothRegisterUser {
    val username: String
    val password: String
}
