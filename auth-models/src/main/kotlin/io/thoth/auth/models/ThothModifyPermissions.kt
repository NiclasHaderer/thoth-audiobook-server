package io.thoth.auth.models

interface ThothModifyPermissions {
    val permissions: ThothUserPermissions
}

data class ThothModifyPermissionsImpl(
    override val permissions: ThothUserPermissions,
) : ThothModifyPermissions
