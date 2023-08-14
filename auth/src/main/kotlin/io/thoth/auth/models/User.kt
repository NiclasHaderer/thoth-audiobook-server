package io.thoth.auth.models

// User models which will be returned by the auth api
interface ThothDatabaseUserPermissions {
    val isAdmin: Boolean
}

interface ThothUser<ID : Any, PERMISSIONS : ThothDatabaseUserPermissions> {
    val id: ID
    val username: String
    val permissions: PERMISSIONS
}

internal class ThothUserImpl<ID : Any, PERMISSIONS : ThothDatabaseUserPermissions>
private constructor(
    override val id: ID,
    override val username: String,
    override val permissions: PERMISSIONS,
) : ThothUser<ID, PERMISSIONS> {
    companion object {
        fun <ID : Any, PERMISSIONS : ThothDatabaseUserPermissions> wrap(
            user: ThothDatabaseUser<ID, PERMISSIONS>
        ): ThothUser<ID, PERMISSIONS> {
            return ThothUserImpl(
                id = user.id,
                username = user.username,
                permissions = user.permissions,
            )
        }
    }
}

// The Internal database model of the user, intentionally does not extend from ThothUser, so the
// internal database
// user cannot be accidentally leaked by returning it instead of the ThothUser

interface ThothDatabaseUser<ID : Any, PERMISSIONS : ThothDatabaseUserPermissions> {
    val id: ID
    val username: String
    val passwordHash: String
    val permissions: PERMISSIONS
}

class ThothDatabaseUserImpl<ID : Any, PERMISSIONS : ThothDatabaseUserPermissions>(
    override val id: ID,
    override val username: String,
    override val passwordHash: String,
    override val permissions: PERMISSIONS,
) : ThothDatabaseUser<ID, PERMISSIONS>
