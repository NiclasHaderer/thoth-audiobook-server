package io.thoth.models

import java.util.*

class UserModel(
    val id: UUID,
    val username: String,
    val passwordHash: String,
    val admin: Boolean,
    val edit: Boolean
)
