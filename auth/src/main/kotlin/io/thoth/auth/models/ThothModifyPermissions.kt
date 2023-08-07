package io.thoth.auth.models

interface ThothModifyPermissions {
    val permissions: Map<String, Any>
    val isAdmin: Boolean
}
