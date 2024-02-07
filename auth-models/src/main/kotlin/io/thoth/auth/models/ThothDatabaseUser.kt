package io.thoth.auth.models

// The Internal database model of the user, intentionally does not extend from ThothUser, so the
// internal database
// user cannot be accidentally leaked by returning it instead of the ThothUser

open class ThothDatabaseUser<ID : Any, PERMISSIONS : ThothUserPermissions>(
    val id: ID,
    val username: String,
    val passwordHash: String,
    val permissions: PERMISSIONS,
)
