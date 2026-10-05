package io.thoth.server.repositories

import java.util.Optional

// An empty Optional is a key the client did not send. Only fields that may be blanked are nullable, and for those a
// null is the client sending null.
class LayerEdit<F : Enum<F>>(
    claimed: Set<F>,
) {
    val claimed: Set<F>
        field = claimed.toMutableSet()

    fun <T> value(
        field: F,
        change: Optional<out T>?,
        current: T?,
    ): T? {
        if (change != null && change.isEmpty) return current
        claimed += field
        return change?.get()
    }

    // The links the layer should hold afterwards, or null when they stay as they are
    fun <T> links(
        field: F,
        change: Optional<out List<T>>?,
    ): List<T>? {
        if (change != null && change.isEmpty) return null
        claimed += field
        return change?.get() ?: emptyList()
    }
}
