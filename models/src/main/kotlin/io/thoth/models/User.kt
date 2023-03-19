package io.thoth.models

import io.thoth.common.serializion.kotlin.UUID_S

data class UserModel(val id: UUID_S, val username: String, val admin: Boolean, val edit: Boolean)

data class InternalUserModel(
    val id: UUID_S,
    val username: String,
    val admin: Boolean,
    val edit: Boolean,
    val passwordHash: String,
)
