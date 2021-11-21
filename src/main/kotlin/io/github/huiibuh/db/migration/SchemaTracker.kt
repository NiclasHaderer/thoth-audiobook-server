package io.github.huiibuh.db.migration

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table

object SchemaTrackers : IntIdTable() {
    val version = integer("version").uniqueIndex()
    val date = long("date")
}

class SchemaTracker(id: EntityID<Int>) : IntEntity(id) {
    companion object: IntEntityClass<SchemaTracker>(SchemaTrackers)
    var version by SchemaTrackers.version
    var date by SchemaTrackers.date
}
