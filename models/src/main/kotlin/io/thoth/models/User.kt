package io.thoth.models

import java.util.*


open class UserModel(
    val id: UUID,
    val username: String,
    val admin: Boolean,
    val edit: Boolean
)

class InternalUserModel(
    id: UUID,
    username: String,
    admin: Boolean,
    edit: Boolean,
    val passwordHash: String,
) : UserModel(
    id,
    username,
    admin,
    edit,
) {
    fun toPublicModel(): UserModel = UserModel(id = id, username = username, admin = admin, edit = edit)
}
