package io.thoth.auth.models

interface ThothUserPermissions {
    val isAdmin: Boolean
}

data class ThothUserPermissionsImpl(
    override val isAdmin: Boolean,
) : ThothUserPermissions
