package io.thoth.models

import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable


@Serializable
data class UserModel(
    val id: UUID_S,
    val username: String,
    val admin: Boolean,
    val edit: Boolean
)

@Serializable
data class InternalUserModel(
    val id: UUID_S,
    val username: String,
    val admin: Boolean,
    val edit: Boolean,
    val passwordHash: String,
)
