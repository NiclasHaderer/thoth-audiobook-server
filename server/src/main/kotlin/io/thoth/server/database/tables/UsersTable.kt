package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object UsersTable : UUIDTable("Users") {
    val username = char("username", 256).uniqueIndex()
    val passwordHash = char("passwordHash", 512)
    val admin = bool("admin").default(false)
}
