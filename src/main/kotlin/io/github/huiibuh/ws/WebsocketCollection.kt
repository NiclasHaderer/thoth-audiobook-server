package io.github.huiibuh.ws

import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.EntityChangeType
import java.util.*

@Serializable
data class ChangeEvent(
    val type: EntityChangeType,
    val id: String,
)

class WebsocketCollection {
    private val connections = Collections.synchronizedList<DefaultWebSocketServerSession>(mutableListOf())

    fun add(connection: DefaultWebSocketServerSession) {
        connections.add(connection)
    }

    fun closeAll() {
        runBlocking {
            connections.forEach {
                it.close(CloseReason(CloseReason.Codes.NORMAL, "server closed connection"))
            }
            connections.clear()
        }
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
        val str = Json.encodeToString(value)
        emit(str)
    }
}
