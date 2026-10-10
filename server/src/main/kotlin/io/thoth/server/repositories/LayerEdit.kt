package io.thoth.server.repositories

import io.thoth.openapi.common.Patch

class LayerEdit<F : Enum<F>>(
    claimed: Set<F>,
) {
    val claimed: Set<F>
        field = claimed.toMutableSet()

    fun <T> value(
        field: F,
        change: Patch<T>,
        current: T,
    ): T =
        when (change) {
            is Patch.Absent -> {
                current
            }

            is Patch.Set -> {
                claimed += field
                change.value
            }
        }

    // The links the layer should hold afterwards, or null when they stay as they are
    fun <T> links(
        field: F,
        change: Patch<List<T>?>,
    ): List<T>? =
        when (change) {
            is Patch.Absent -> {
                null
            }

            is Patch.Set -> {
                claimed += field
                change.value ?: emptyList()
            }
        }
}
