package io.github.huiibuh.ws

import io.ktor.routing.*
import io.ktor.websocket.*
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.sql.Table

enum class NotificationType(val changeType: Set<EntityChangeType>) {
    UPDATE(setOf(EntityChangeType.Updated, EntityChangeType.Created)),
    DELETE(setOf(EntityChangeType.Removed)),
    ALL(setOf(EntityChangeType.Created, EntityChangeType.Removed, EntityChangeType.Updated))
}


fun Route.withNotifications(path: String, table: Table, type: NotificationType) {
    val sockets = WebsocketCollection()

    EntityHook.subscribe {
        if (type.changeType.contains(it.changeType)) {
            if (it.entityClass.table == table) {
                sockets.emit(ChangeEvent(
                    type = it.changeType,
                    ids = it.entityId.value.toString(),
                    data = null
                ))
            }
        }
    }

    webSocket(path) {
        sockets.add(this)
    }

}

fun Route.updateForTables(vararg tables: Table) {
    for (table in tables) {
        withNotifications("/${table.tableName.lowercase()}", table, NotificationType.ALL)
    }
}
