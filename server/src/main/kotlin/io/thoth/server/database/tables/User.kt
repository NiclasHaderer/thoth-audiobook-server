package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

object TUsers : UUIDTable("Users") {
    val username = char("username", 256).uniqueIndex()
    val passwordHash = char("passwordHash", 512)
    val admin = bool("admin").default(false)
}

class User(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(TUsers)

    var username by TUsers.username
    var passwordHash by TUsers.passwordHash
    var admin by TUsers.admin
    val permissions by LibraryUserMappingEntity referrersOn TLibraryUserMapping.user
}
