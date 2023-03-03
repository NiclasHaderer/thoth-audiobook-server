package io.thoth.server.ws

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.sql.Table

enum class NotificationType(val changeType: Set<EntityChangeType>) {
    UPDATE(setOf(EntityChangeType.Updated, EntityChangeType.Created)),
    DELETE(setOf(EntityChangeType.Removed)),
    ALL(setOf(EntityChangeType.Created, EntityChangeType.Removed, EntityChangeType.Updated))
}

fun Route.withNotifications(path: String, table: Table, type: NotificationType) {
    val collection = WebsocketCollection()

    EntityHook.subscribe {
        // Not correct change type of wrong table
        if (!type.changeType.contains(it.changeType) || it.entityClass.table != table)
            return@subscribe
        runBlocking {
            collection.emit(
                ChangeEvent(
                    type = it.changeType,
                    id = it.entityId.value.toString(),
                )
            )
        }
    }

    webSocket(path) {
        collection.add(this)
        this.closeReason.await()
        collection.remove(this)
    }
}

fun Route.updateForTables(vararg tables: Table) {
    for (table in tables) {
        withNotifications("/${table.tableName.lowercase()}", table, NotificationType.ALL)
    }
}
