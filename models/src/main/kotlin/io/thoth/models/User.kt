package io.thoth.models

import java.util.*

data class UserModel(val id: UUID, val username: String, val permissions: UserPermissionsModel)

data class InternalUserModel(
    val id: UUID,
    val username: String,
    val passwordHash: String,
    val permissions: UserPermissionsModel
)
