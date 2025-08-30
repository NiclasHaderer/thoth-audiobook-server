package io.thoth.server.database.migrations

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object SchemaTrackerTable : IntIdTable("SchemaTracker") {
    val version = integer("version").uniqueIndex()
    val date = long("date")
}
