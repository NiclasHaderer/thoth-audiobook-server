package io.thoth.server.database.tables

import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.models.UserModel
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class UserEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(UsersTable)

    var username by UsersTable.username
    var passwordHash by UsersTable.passwordHash
    var admin by UsersTable.admin
    val permissions by LibraryUserEntity referrersOn LibraryUserTable.user

    fun toModel(): UserModel = UserModel(id = id.value, username = username, admin = admin)

    fun toExternalUser(): ThothDatabaseUser =
        ThothDatabaseUser(
            id = id.value,
            username = username,
            passwordHash = passwordHash,
        )
}
