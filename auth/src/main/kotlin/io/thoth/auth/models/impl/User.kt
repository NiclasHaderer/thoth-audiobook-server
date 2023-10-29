package io.thoth.auth.models.impl

import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserPermissions

internal data class ThothUserImpl<ID : Any, PERMISSIONS : ThothUserPermissions>(
    override val id: ID,
    override val username: String,
    override val permissions: PERMISSIONS,
) : ThothUser<ID, PERMISSIONS> {
    internal companion object {
        fun <ID : Any, PERMISSIONS : ThothUserPermissions> wrap(
            user: ThothDatabaseUser<ID, PERMISSIONS>
        ): ThothUser<ID, PERMISSIONS> {
            return ThothUserImpl(
                id = user.id,
                username = user.username,
                permissions = user.permissions,
            )
        }
    }
}

internal data class ThothDatabaseUserImpl<ID : Any, PERMISSIONS : ThothUserPermissions>(
    override val id: ID,
    override val username: String,
    override val passwordHash: String,
    override val permissions: PERMISSIONS,
) : ThothDatabaseUser<ID, PERMISSIONS>
