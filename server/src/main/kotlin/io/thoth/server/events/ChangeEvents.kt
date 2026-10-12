package io.thoth.server.events

import io.thoth.models.ChangeEvent
import io.thoth.models.ChangeEventBatch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onSubscription
import java.util.concurrent.atomic.AtomicInteger

internal const val BUFFERED_BATCHES = 256

class ChangeEvents {
    private class Published(
        val sequence: Long,
        val events: List<ChangeEvent>,
    )

    // Published from the committing thread, which must never wait for a slow listener
    private val published =
        MutableSharedFlow<Published>(
            extraBufferCapacity = BUFFERED_BATCHES,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    @Volatile
    private var sequence = 0L

    private val permissionChanges = AtomicInteger()

    // Changes whenever who may read which library changed, so a listener can keep what it resolved until then
    val permissionsVersion: Int get() = permissionChanges.get()

    fun permissionsChanged() {
        permissionChanges.incrementAndGet()
    }

    fun publish(events: List<ChangeEvent>) =
        synchronized(this) {
            published.tryEmit(Published(++sequence, events))
        }

    fun listen(onListening: suspend () -> Unit = {}): Flow<ChangeEventBatch> =
        flow {
            var last = 0L
            published
                .onSubscription {
                    last = sequence
                    onListening()
                }.collect {
                    emit(ChangeEventBatch(it.events, missedPreviousEvents = it.sequence > last + 1))
                    last = it.sequence
                }
        }
}
