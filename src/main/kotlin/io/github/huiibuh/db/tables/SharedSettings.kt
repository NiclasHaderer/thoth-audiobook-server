package io.github.huiibuh.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable

object TKeyValueSettings : UUIDTable("KeyValueSettings") {
    val scanIndex = integer("scanIndex").default(0)
}
