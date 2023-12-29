package io.thoth.auth.utils

import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.auth.models.ThothUser
import io.thoth.auth.models.ThothUserImpl
import io.thoth.auth.models.ThothUserPermissions

fun <ID: Any, PERMISSIONS: ThothUserPermissions>ThothDatabaseUser<ID, PERMISSIONS>.wrap(): ThothUser<ID, PERMISSIONS> {
    return ThothUserImpl(
        id = this.id,
        username = this.username,
        permissions = this.permissions,
    )
}
