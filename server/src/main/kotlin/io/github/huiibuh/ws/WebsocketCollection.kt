package io.github.huiibuh.ws

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import org.jetbrains.exposed.dao.EntityChangeType
import java.util.*


class ChangeEvent(
    val type: EntityChangeType,
    val id: String,
)

class WebsocketCollection {
    private val connections = Collections.synchronizedList<DefaultWebSocketServerSession>(mutableListOf())
    private val mapper = ObjectMapper()

    fun add(connection: DefaultWebSocketServerSession) {
        connections.add(connection)
    }

    suspend fun closeAll() {
        connections.forEach {
            it.close(CloseReason(CloseReason.Codes.NORMAL, "server closed connection"))
        }
        connections.clear()
    }

    fun remove(conn: DefaultWebSocketServerSession) {
        connections.remove(conn)
    }

    suspend fun emit(value: String) {
        connections.forEach {
            it.send(value)
        }
    }

    suspend fun emit(value: ChangeEvent) {
        val str = mapper.writeValueAsString(value)
        emit(str)
    }
}
