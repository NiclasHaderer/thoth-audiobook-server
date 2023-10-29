package io.thoth.auth.models.impl

import io.thoth.auth.models.RegisteredUser

internal data class RegisteredUserImpl(
    override val username: String,
    override val passwordHash: String,
    override val admin: Boolean,
) : RegisteredUser
