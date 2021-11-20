package io.github.huiibuh.ws

import io.ktor.routing.*
import io.ktor.websocket.*
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.sql.Table

enum class NotificationType(val changeType: Set<EntityChangeType>) {
    UPDATE(setOf(EntityChangeType.Updated, EntityChangeType.Created)),
    DELETE(setOf(EntityChangeType.Removed)),
}


fun Route.withNotifications(path: String, table: Table, type: NotificationType) {
    val sockets = WebsocketCollection()

    EntityHook.subscribe {
        if (type.changeType.contains(it.changeType)) {
            if (it.entityClass.table == table) {
                sockets.emit(ChangeEvent(
                    type = it.changeType.toString(),
                    ids = it.entityId.value.toString(),
                    data = null
                ))
            }
        }
    }

    // TODO pass data of change object to the event
    //    if (it.entityClass is UUIDEntityClass) {
    //        val id = (it.entityId as EntityID<UUID>).value
    //        val model = (it.entityClass as UUIDEntityClass).findById(id)
    //        Json.encodeToString((model as ToModel<*>).toModel() as ArtistModel)
    //    } else {
    //        null
    //    }
    //    ))

    webSocket(path) {
        sockets.add(this)
    }

}

fun Route.updateForTables(vararg tables: Table) {
    for (table in tables) {
        withNotifications("/${table.tableName.lowercase()}/update", table, NotificationType.UPDATE)
        withNotifications("/${table.tableName.lowercase()}/delete", table, NotificationType.DELETE)
    }
}
