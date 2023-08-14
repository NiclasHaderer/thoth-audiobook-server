package io.thoth.auth.models

interface ThothModifyPermissions {
    val permissions: ThothUserPermissions
    val isAdmin: Boolean
}
