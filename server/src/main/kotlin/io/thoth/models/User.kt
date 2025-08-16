package io.thoth.models

import java.util.UUID

data class UserModel(
    val id: UUID,
    val username: String,
    val admin: Boolean,
)
