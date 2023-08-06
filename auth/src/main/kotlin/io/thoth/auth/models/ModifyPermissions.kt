package io.thoth.auth.models

interface ModifyPermissions {
    val permissions: Map<String, Any>
    val isAdmin: Boolean
}
