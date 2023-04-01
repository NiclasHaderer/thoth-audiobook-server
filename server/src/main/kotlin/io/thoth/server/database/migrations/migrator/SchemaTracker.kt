package io.thoth.server.database.migrations.migrator

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object TSchemaTrackers : IntIdTable("SchemaTrackers") {
    val version = integer("version").uniqueIndex()
    val date = long("date")
    val rollback = text("rollback")
}

class SchemaTracker(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SchemaTracker>(TSchemaTrackers)

    var version by TSchemaTrackers.version
    var date by TSchemaTrackers.date
    var rollback by TSchemaTrackers.rollback
}
