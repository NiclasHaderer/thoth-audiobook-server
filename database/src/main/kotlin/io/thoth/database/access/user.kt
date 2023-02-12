package io.thoth.database.access

import io.thoth.common.extensions.findOne
import io.thoth.database.tables.TUsers
import io.thoth.database.tables.User
import io.thoth.models.InternalUserModel
import io.thoth.models.UserModel
import java.util.*

fun User.Companion.getById(uuid: UUID): UserModel? {
    return User.findById(uuid)?.toModel()
}

fun User.Companion.internalGetById(uuid: UUID): UserModel? {
    return User.findById(uuid)?.toModel()
}

fun User.Companion.getByName(name: String): UserModel? {
    return findOne { TUsers.username like name }?.toModel()
}

fun User.Companion.internalGetByName(name: String): InternalUserModel? {
    return findOne { TUsers.username like name }?.toInternalModel()
}

fun User.toInternalModel() = InternalUserModel(
    id = id.value,
    username = username,
    admin = admin,
    edit = edit,
    passwordHash = passwordHash
)

fun User.toModel(): UserModel = UserModel(id = id.value, username = username, admin = admin, edit = edit)
