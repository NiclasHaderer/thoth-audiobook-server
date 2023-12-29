package io.thoth.auth.models

interface ThothRenameUser {
    val username: String
}

data class ThothRenameUserImpl(
    override val username: String,
) : ThothRenameUser
