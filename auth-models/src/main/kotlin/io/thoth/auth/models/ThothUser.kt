package io.thoth.auth.models

// User models which will be returned by the auth api

interface ThothUser<ID : Any, PERMISSIONS : ThothUserPermissions> {
    val id: ID
    val username: String
    val permissions: PERMISSIONS
}

data class ThothUserImpl<ID : Any, PERMISSIONS : ThothUserPermissions>(
    override val id: ID,
    override val username: String,
    override val permissions: PERMISSIONS,
) : ThothUser<ID, PERMISSIONS>
