package io.thoth.openapi.common

// A request field that may be left out. Absent is a key the client did not send, Set carries what it sent, which is
// null only where T allows one.
sealed interface Patch<out T> {
    data object Absent : Patch<Nothing>

    data class Set<out T>(
        val value: T,
    ) : Patch<T>
}

// Leaves the key out for null, so it can never send an explicit null: that takes Patch.Set(null)
fun <T : Any> T?.orAbsent(): Patch<T> = if (this == null) Patch.Absent else Patch.Set(this)

val Patch<*>.isSet: Boolean get() = this is Patch.Set

fun <T> Patch<T>.orElse(default: T): T = if (this is Patch.Set) value else default

inline fun <T> Patch<T>.ifSet(block: (T) -> Unit) {
    if (this is Patch.Set) block(value)
}

inline fun <T, R> Patch<T>.map(transform: (T) -> R): Patch<R> =
    if (this is Patch.Set) Patch.Set(transform(value)) else Patch.Absent
