package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

object ImageTable : UUIDTable("image") {
    val image = blob("image")
}
