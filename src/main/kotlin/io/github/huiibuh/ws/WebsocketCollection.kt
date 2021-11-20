package io.github.huiibuh.ws

import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
data class ChangeEvent(
    val type: String,
    val ids: String,
    val data: String?
)

class WebsocketCollection() {
    private val connections = Collections.synchronizedList<DefaultWebSocketServerSession>(mutableListOf())

    fun add(connection: DefaultWebSocketServerSession) {
        connections.add(connection)
        runBlocking {
            connection.closeReason.await()
            connections.remove(connection)
        }
    }

    fun closeAll() {
        runBlocking {
            connections.forEach {
                it.close(CloseReason(CloseReason.Codes.NORMAL, "server closed connection"))
            }
            connections.clear()
        }
    }

    fun emit(value: String) {
        runBlocking {
            connections.forEach {
                it.send(value)
            }
        }
    }

    fun emit(value: ChangeEvent) {
        val str = Json.encodeToString(value)
        emit(str)
    }
}
