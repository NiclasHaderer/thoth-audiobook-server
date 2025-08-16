package io.thoth.auth.models

import java.util.UUID

// The Internal database model of the user, intentionally does not extend from ThothUser, so the
// internal database
// user cannot be accidentally leaked by returning it instead of the ThothUser

open class ThothDatabaseUser(
    open val id: UUID,
    open val username: String,
    open val passwordHash: String,
)
