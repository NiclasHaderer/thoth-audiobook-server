package io.thoth.server.repositories

import io.thoth.openapi.common.Patch
import io.thoth.openapi.common.orElse
import io.thoth.openapi.ktor.errors.ErrorResponse

class LayerEdit<F : Enum<F>>(
    claimed: Set<F>,
    reset: Patch<List<F>>,
) {
    val claimed: Set<F>
        field = claimed.toMutableSet()

    private val reset = reset.orElse(emptyList()).toSet()

    fun <T> value(
        field: F,
        change: Patch<T>,
        current: T?,
        resolved: T?,
    ): T? =
        when {
            field in reset -> {
                release(field, change)
                null
            }

            change !is Patch.Set -> {
                current
            }

            change.value == resolved -> {
                current
            }

            else -> {
                claimed += field
                change.value
            }
        }

    // The links the layer should hold afterwards, or null when they stay as they are
    fun <T> links(
        field: F,
        change: Patch<List<T>?>,
        resolved: Collection<T>,
    ): List<T>? {
        if (field in reset) {
            release(field, change)
            return emptyList()
        }
        if (change !is Patch.Set) return null
        val wanted = change.value ?: emptyList()
        if (wanted.toSet() == resolved.toSet()) return null
        claimed += field
        return wanted
    }

    private fun release(
        field: F,
        change: Patch<*>,
    ) {
        if (change is Patch.Set) {
            throw ErrorResponse.userError("$field cannot be set and reset in the same request, drop one of the two")
        }
        claimed -= field
    }
}
