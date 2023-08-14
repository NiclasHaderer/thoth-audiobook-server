package io.thoth.server.database.tables

import java.util.*
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object TUsers : UUIDTable("Users") {
    val username = char("username", 256).uniqueIndex()
    val passwordHash = char("passwordHash", 512)
    val permissions = reference("permissions", TUserPermissions, onDelete = ReferenceOption.CASCADE)
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(TUsers)

    var username by TUsers.username
    var passwordHash by TUsers.passwordHash
    val permissions by UserPermissions referencedOn TUsers.permissions
}
