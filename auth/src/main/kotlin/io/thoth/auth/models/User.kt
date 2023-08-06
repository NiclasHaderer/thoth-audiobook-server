package io.thoth.auth.models

interface ThothDatabaseUser {
    val id: Any
    val username: String
    val passwordHash: String
    val admin: Boolean
    val permissions: Any
}

interface ThothUser {
    val id: Any
    val username: String
    val admin: Boolean
    val permissions: Any
}

internal class ThothUserImpl
private constructor(
    override val id: Any,
    override val username: String,
    override val admin: Boolean,
    override val permissions: Any,
) : ThothUser {
    companion object {
        fun wrap(user: ThothDatabaseUser): ThothUser {
            return ThothUserImpl(
                id = user.id,
                username = user.username,
                admin = user.admin,
                permissions = user.permissions,
            )
        }
    }
}
