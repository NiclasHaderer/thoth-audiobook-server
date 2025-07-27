package io.thoth.server.ws

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.EntityChangeType

@Serializable class ChangeEvent(val type: EntityChangeType, val id: String)

class WebsocketCollection {
    private val connections = Collections.synchronizedList<DefaultWebSocketServerSession>(mutableListOf())

    fun add(connection: DefaultWebSocketServerSession) {
        connections.add(connection)
    }

    suspend fun closeAll() {
        connections.forEach { it.close(CloseReason(CloseReason.Codes.NORMAL, "server closed connection")) }
        connections.clear()
    }

    fun remove(conn: DefaultWebSocketServerSession) {
        connections.remove(conn)
    }

    suspend fun emit(value: ChangeEvent) {
        connections.forEach { it.sendSerialized(value) }
    }
}
