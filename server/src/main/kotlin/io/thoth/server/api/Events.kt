package io.thoth.server.api

import io.ktor.server.auth.principal
import io.ktor.server.routing.Routing
import io.ktor.server.websocket.sendSerialized
import io.ktor.websocket.DefaultWebSocketSession
import io.thoth.models.ChangeEvent
import io.thoth.models.ChangeEventBatch
import io.thoth.models.ChangeType
import io.thoth.models.ChangedEntity
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.ktor.webSocket
import io.thoth.server.events.ChangeEvents
import io.thoth.server.plugins.auth.ThothPrincipalImpl
import io.thoth.server.plugins.auth.resolveUserPermissions
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject

fun Routing.eventRouting() {
    val changeEvents by inject<ChangeEvents>()

    webSocket<Api.Events> {
        val userId =
            call.principal<ThothPrincipalImpl>()?.userId
                ?: throw ErrorResponse.internalError("Could not get principal of the event socket")

        val forwarding =
            launch {
                // Tells the client from when on no commit is missed, the handshake finishes earlier
                val listening = ChangeEventBatch(emptyList())
                changeEvents.batches.onSubscription { sendSerialized(listening) }.collect { batch ->
                    val readable = resolveUserPermissions(userId).libraries.mapTo(mutableSetOf()) { it.id }
                    val events = batch.filter { it.libraryId in readable || it.isDeletedLibrary }
                    if (events.isNotEmpty()) sendSerialized(ChangeEventBatch(events))
                }
            }
        waitForClose()
        forwarding.cancel()
    }
}

// Sent to everyone: the permissions that would say who may know are deleted with the library
private val ChangeEvent.isDeletedLibrary get() = entity == ChangedEntity.LIBRARY && type == ChangeType.DELETED

private suspend fun DefaultWebSocketSession.waitForClose() = incoming.consumeEach { }
