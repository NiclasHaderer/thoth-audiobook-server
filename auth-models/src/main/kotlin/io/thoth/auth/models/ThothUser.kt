package io.thoth.auth.models

// User models which will be returned by the auth api

open class ThothUser<ID : Any, PERMISSIONS : ThothUserPermissions>(
    val id: ID,
    val username: String,
    val permissions: PERMISSIONS,
)
