package io.thoth.server.database.access

import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.models.UserModel
import io.thoth.models.UserPermissionsModel
import io.thoth.server.database.tables.User
import java.util.*

fun User.wrap(): ThothDatabaseUser<UUID, UserPermissionsModel> {
    return object : ThothDatabaseUser<UUID, UserPermissionsModel> {
        override val id = this@wrap.id.value
        override val username = this@wrap.username
        override val passwordHash = this@wrap.passwordHash
        override val permissions = this@wrap.permissions.toModel()
    }
}

fun User.toModel(): UserModel = UserModel(id = id.value, username = username, permissions = permissions.toModel())
