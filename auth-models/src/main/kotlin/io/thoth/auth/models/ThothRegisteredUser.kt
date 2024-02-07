package io.thoth.auth.models

open class ThothRegisteredUser(
    val username: String,
    val passwordHash: String,
    val admin: Boolean,
)
