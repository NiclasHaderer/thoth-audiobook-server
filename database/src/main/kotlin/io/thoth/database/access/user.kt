package io.thoth.database.access

import io.thoth.common.extensions.findOne
import io.thoth.database.tables.TUsers
import io.thoth.database.tables.User
import io.thoth.models.InternalUserModel
import java.util.*

fun User.Companion.getById(uuid: UUID): InternalUserModel? {
    return User.findById(uuid)?.toModel()
}

fun User.Companion.getByName(name: String): InternalUserModel? {
    return findOne { TUsers.username like name }?.toModel()
}

fun User.toModel() = InternalUserModel(
    id = id.value,
    username = username,
    admin = admin,
    edit = edit,
    passwordHash = passwordHash
)