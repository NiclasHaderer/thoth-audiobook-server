package io.github.huiibuh.db.update.interceptor

import io.github.huiibuh.extensions.classLogger
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.dao.toEntity
import org.jetbrains.exposed.sql.Table
import java.time.LocalDateTime
import java.util.*

interface TimeUpdatable {
    var updateTime: LocalDateTime
}


fun withUpdateTime(vararg tables: Table) {
    EntityHook.subscribe {
        val table = it.entityClass.table
        val hasTable = tables.contains(table)
        val isUpdate = it.changeType == EntityChangeType.Updated

        if (!hasTable || !isUpdate) return@subscribe
        val entity = (it.toEntity<UUID, Entity<UUID>>()) ?: return@subscribe
        if (entity is TimeUpdatable) {
            entity.updateTime = LocalDateTime.now()
        } else {
            val logger = table.classLogger()
            logger.error("Table ${table.tableName} has no Entity with implements TimeUpdatable")
        }
    }
}
