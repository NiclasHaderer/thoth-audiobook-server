package io.thoth.server.repositories

import io.thoth.openapi.common.Patch
import io.thoth.openapi.common.orElse
import io.thoth.openapi.ktor.errors.ErrorResponse

// The user always gets to write, and locks every field they change against later scans and matches
class UserEdit<F : Enum<F>>(
    locked: Set<F>,
    unlock: Patch<List<F>>,
) {
    private val unlock = unlock.orElse(emptyList()).toSet()

    val locked: Set<F>
        field = (locked - this.unlock).toMutableSet()

    // A value equal to the current one is not an edit: locking it would pin whatever the files or the agent
    // say today and stop later scans and matches from updating it.
    fun <T> value(
        field: F,
        current: T,
        change: Patch<T>,
    ): T {
        rejectUnlocked(field, change)
        if (change !is Patch.Set || change.value == current) return current
        locked += field
        return change.value
    }

    // The links the entity should hold afterwards, or null when they stay as they are
    fun <T> links(
        field: F,
        current: Collection<T>,
        change: Patch<List<T>?>,
    ): List<T>? {
        rejectUnlocked(field, change)
        if (change !is Patch.Set) return null
        val wanted = change.value ?: emptyList()
        if (wanted.toSet() == current.toSet()) return null
        locked += field
        return wanted
    }

    private fun rejectUnlocked(
        field: F,
        change: Patch<*>,
    ) {
        if (field in unlock && change is Patch.Set) {
            throw ErrorResponse.userError("$field cannot be set and unlocked in the same request, drop one of the two")
        }
    }
}

class AutomaticWrite<F : Enum<F>>(
    private val locked: Set<F>,
    private val onlyFillEmpty: Boolean,
) {
    fun <T> value(
        field: F,
        current: T,
        incoming: (T) -> T?,
    ): T = if (onlyFillEmpty && current != null) current else overwrite(field, current, incoming)

    fun <T> overwrite(
        field: F,
        current: T,
        incoming: (T) -> T?,
    ): T = if (mayWrite(field)) incoming(current) ?: current else current

    fun mayWrite(field: F): Boolean = field !in locked
}
