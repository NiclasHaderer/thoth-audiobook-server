package io.thoth.auth.models

interface ThothDatabaseUser {
    val id: Any
    val username: String
    val passwordHash: String
    val admin: Boolean
    val permissions: Map<String, Any>
}

class ThothDatabaseUserImpl(
    override val id: Any,
    override val username: String,
    override val passwordHash: String,
    override val admin: Boolean,
    override val permissions: Map<String, Any>,
) : ThothDatabaseUser

interface ThothUser {
    val id: Any
    val username: String
    val admin: Boolean
    val permissions: Map<String, Any>
}

interface ThothRenameUser {
    val username: String
}

internal class ThothUserImpl
private constructor(
    override val id: Any,
    override val username: String,
    override val admin: Boolean,
    override val permissions: Map<String, Any>,
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
