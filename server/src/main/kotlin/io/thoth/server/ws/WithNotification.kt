package io.thoth.server.ws

import io.ktor.routing.*
import io.ktor.websocket.*
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
    val sockets = WebsocketCollection()

    EntityHook.subscribe {
        if (type.changeType.contains(it.changeType)) {
            if (it.entityClass.table == table) {
                runBlocking {
                    sockets.emit(
                        ChangeEvent(
                            type = it.changeType,
                            id = it.entityId.value.toString(),
                        )
                    )
                }
            }
        }
    }

    webSocket(path) {
        sockets.add(this)
        this.closeReason.await()
        sockets.remove(this)
    }

}

fun Route.updateForTables(vararg tables: Table) {
    for (table in tables) {
        withNotifications("/${table.tableName.lowercase()}", table, NotificationType.ALL)
    }
}
