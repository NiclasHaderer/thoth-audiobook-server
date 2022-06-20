package io.thoth.database.tables

import io.thoth.common.extensions.findOne
import io.thoth.database.ToModel
import io.thoth.models.InternalUserModel
import io.thoth.models.UserModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


object TUsers : UUIDTable("Users") {
    val username = char("username", 256).uniqueIndex()
    val passwordHash = char("passwordHash", 512)
    val admin = bool("admin").default(false)
    val edit = bool("edit").default(false)
}


class User(id: EntityID<UUID>) : UUIDEntity(id), ToModel<UserModel> {
    companion object : UUIDEntityClass<User>(TUsers) {
        fun getById(uuid: UUID) = transaction {
            findById(uuid)?.toModel()
        }

        fun getByName(name: String) = transaction {
            findOne { TUsers.username like name }?.toModel()
        }
    }

    var username by TUsers.username
    var passwordHash by TUsers.passwordHash
    var admin by TUsers.admin
    var edit by TUsers.edit

    override fun toModel() = InternalUserModel(
        id = id.value,
        username = username,
        admin = admin,
        edit = edit,
        passwordHash = passwordHash
    )
}

object TUserTokens : UUIDTable("UserTokens") {
    val user = reference("user", TUsers)
    val valid = bool("valid")
    val token = text("token")
}

class UserToken(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserToken>(TUsers)

    var user by User referencedOn TUserTokens.user
    var valid by TUserTokens.valid
    var token by TUserTokens.token
}

