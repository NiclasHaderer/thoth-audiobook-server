package io.thoth.auth.models

// User models which will be returned by the auth api

open class ThothUser<ID : Any, PERMISSIONS : ThothUserPermissions>(
    open val id: ID,
    open val username: String,
    open val permissions: PERMISSIONS,
)
