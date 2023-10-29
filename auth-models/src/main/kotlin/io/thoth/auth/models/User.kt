package io.thoth.auth.models

// User models which will be returned by the auth api

interface ThothUser<ID : Any, PERMISSIONS : ThothUserPermissions> {
    val id: ID
    val username: String
    val permissions: PERMISSIONS
}

// The Internal database model of the user, intentionally does not extend from ThothUser, so the
// internal database
// user cannot be accidentally leaked by returning it instead of the ThothUser

interface ThothDatabaseUser<ID : Any, PERMISSIONS : ThothUserPermissions> {
    val id: ID
    val username: String
    val passwordHash: String
    val permissions: PERMISSIONS
}
