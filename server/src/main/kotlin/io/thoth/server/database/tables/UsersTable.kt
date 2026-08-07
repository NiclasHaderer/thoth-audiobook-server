package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

object UsersTable : UUIDTable("Users") {
    val username = varchar("username", 255).uniqueIndex()
    val passwordHash = varchar("passwordHash", 255)
    val admin = bool("admin").default(false)
}
