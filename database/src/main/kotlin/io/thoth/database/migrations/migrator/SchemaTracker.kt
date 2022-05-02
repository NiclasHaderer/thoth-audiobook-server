package io.thoth.database.migrations.migrator

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object TSchemaTrackers : IntIdTable("SchemaTrackers") {
    val version = integer("version").uniqueIndex()
    val date = long("date")
}

class SchemaTracker(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SchemaTracker>(TSchemaTrackers)

    var version by TSchemaTrackers.version
    var date by TSchemaTrackers.date
}
