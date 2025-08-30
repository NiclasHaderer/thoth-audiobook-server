package io.thoth.server.database.migrations

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class SchemaTrackerEntity(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<SchemaTrackerEntity>(SchemaTrackerTable)

    var version by SchemaTrackerTable.version
    var date by SchemaTrackerTable.date
}
