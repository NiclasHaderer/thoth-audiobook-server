package io.thoth.server.database.migrations

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import io.thoth.server.database.extensions.timestampMillis

object SchemaTrackerTable : IntIdTable("SchemaTracker") {
    val version = integer("version").uniqueIndex()
    val appliedAt = timestampMillis("appliedAt")
}
