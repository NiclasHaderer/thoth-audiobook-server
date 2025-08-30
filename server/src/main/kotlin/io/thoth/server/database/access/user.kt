package io.thoth.server.database.access

import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.models.UserModel
import io.thoth.server.database.tables.UserEntity

fun UserEntity.toExternalUser(): ThothDatabaseUser =
    ThothDatabaseUser(
        id = this@toExternalUser.id.value,
        username = this@toExternalUser.username,
        passwordHash = this@toExternalUser.passwordHash,
    )

fun UserEntity.toModel(): UserModel = UserModel(id = id.value, username = username, admin = admin)
