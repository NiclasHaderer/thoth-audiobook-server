package io.thoth.server.database.access

import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.auth.models.ThothDatabaseUserImpl
import io.thoth.models.InternalUserModel
import io.thoth.models.UserModel
import io.thoth.models.UserPermissionsModel
import io.thoth.server.common.extensions.findOne
import io.thoth.server.database.tables.User
import java.util.*

fun User.Companion.getById(uuid: UUID): UserModel? {
    return User.findById(uuid)?.toModel()
}

fun User.Companion.getByName(name: String): UserModel? {
    return findOne { io.thoth.server.database.tables.TUsers.username like name }?.toModel()
}

fun User.Companion.internalGetByName(name: String): InternalUserModel? {
    return findOne { io.thoth.server.database.tables.TUsers.username like name }?.toInternalModel()
}

fun User.wrap(): ThothDatabaseUser<UUID, UserPermissionsModel> {
    return ThothDatabaseUserImpl(
        id = id.value,
        username = username,
        passwordHash = passwordHash,
        permissions = permissions.toModel()
    )
}

fun User.toInternalModel() =
    InternalUserModel(
        id = id.value,
        username = username,
        passwordHash = passwordHash,
        permissions = permissions.toModel()
    )

fun User.toModel(): UserModel = UserModel(id = id.value, username = username, permissions = permissions.toModel())
