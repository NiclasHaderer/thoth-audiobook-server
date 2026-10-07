package io.thoth.server.repositories

import io.thoth.openapi.common.Patch
import io.thoth.openapi.common.orElse
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.tables.LayerRow
import io.thoth.server.database.tables.Layers

class LayerEdit<R : LayerRow<F>, F : Enum<F>>(
    private val layers: Layers<R, F>,
    reset: Patch<List<F>>,
) {
    val user: R get() = layers.user

    val claimed: Set<F>
        field = layers.user.claimed.toMutableSet()

    private val reset = reset.orElse(emptyList()).toSet()

    // A value equal to what the entity already resolves to is not an edit: claiming it would pin whatever a lower
    // layer says today and stop later scans and matches from updating it.
    fun <T> value(
        field: F,
        change: Patch<T>,
        get: R.() -> T?,
    ): T? {
        val current = layers.user.get()
        return when {
            field in reset -> {
                release(field, change)
                null
            }

            change !is Patch.Set -> {
                current
            }

            change.value == layers.resolve(field, get) -> {
                current
            }

            else -> {
                claimed += field
                change.value
            }
        }
    }

    // The links the user layer should hold afterwards, or null when they stay as they are
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
