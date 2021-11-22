package io.github.huiibuh.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable

object TSharedSettings : UUIDTable("SharedSettings") {
    val scanIndex = integer("scanIndex").default(0)
}
