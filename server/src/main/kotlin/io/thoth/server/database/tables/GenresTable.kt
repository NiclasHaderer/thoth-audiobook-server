package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

object GenresTable : UUIDTable("Genres") {
    val name = varchar("name", 255)
}
