package io.thoth.auth.models

// The Internal database model of the user, intentionally does not extend from ThothUser, so the
// internal database
// user cannot be accidentally leaked by returning it instead of the ThothUser

open class ThothDatabaseUser<ID : Any, PERMISSIONS : ThothUserPermissions>(
    open val id: ID,
    open val username: String,
    open val passwordHash: String,
    open val permissions: PERMISSIONS,
)
