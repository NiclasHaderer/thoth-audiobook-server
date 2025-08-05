package io.thoth.server.database.access

import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.models.UserModel
import io.thoth.server.database.tables.User

fun User.toExternalUser(): ThothDatabaseUser {
    return ThothDatabaseUser(
        id = this@toExternalUser.id.value,
        username = this@toExternalUser.username,
        passwordHash = this@toExternalUser.passwordHash,
    )
}

fun User.toModel(): UserModel = UserModel(id = id.value, username = username, permissions = permissions.toModel())
