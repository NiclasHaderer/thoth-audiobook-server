package io.thoth.auth.models

interface ThothDatabaseUser<T : Any> {
    val id: T
    val username: String
    val passwordHash: String
    val admin: Boolean
    val permissions: Map<String, Any>
}

class ThothDatabaseUserImpl<T : Any>(
    override val id: T,
    override val username: String,
    override val passwordHash: String,
    override val admin: Boolean,
    override val permissions: Map<String, Any>,
) : ThothDatabaseUser<T>

interface ThothUser<T : Any> {
    val id: T
    val username: String
    val admin: Boolean
    val permissions: Map<String, Any>
}

interface ThothRenameUser {
    val username: String
}

internal class ThothUserImpl<T : Any>
private constructor(
    override val id: T,
    override val username: String,
    override val admin: Boolean,
    override val permissions: Map<String, Any>,
) : ThothUser<T> {
    companion object {
        fun <T : Any> wrap(user: ThothDatabaseUser<T>): ThothUser<T> {
            return ThothUserImpl(
                id = user.id,
                username = user.username,
                admin = user.admin,
                permissions = user.permissions,
            )
        }
    }
}
