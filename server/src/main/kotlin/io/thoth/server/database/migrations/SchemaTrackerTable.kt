package io.thoth.server.database.migrations

import io.thoth.server.database.extensions.timestampMillis
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object SchemaTrackerTable : IntIdTable("schema_tracker") {
    val version = integer("version").uniqueIndex()
    val appliedAt = timestampMillis("applied_at")
}
