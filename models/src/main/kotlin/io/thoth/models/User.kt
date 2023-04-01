package io.thoth.models

import java.util.*

data class UserModel(val id: UUID, val username: String, val admin: Boolean, val edit: Boolean)

data class InternalUserModel(
    val id: UUID,
    val username: String,
    val admin: Boolean,
    val edit: Boolean,
    val passwordHash: String,
)
