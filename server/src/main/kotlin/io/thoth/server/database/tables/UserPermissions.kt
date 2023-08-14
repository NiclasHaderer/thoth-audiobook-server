package io.thoth.server.database.tables

import java.util.*
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable

object TUserPermissions : UUIDTable("TUserPermissions") {
    val isAdmin = bool("isAdmin")
}

class UserPermissions(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserPermissions>(TUserPermissions)

    var isAdmin by TUserPermissions.isAdmin
}
