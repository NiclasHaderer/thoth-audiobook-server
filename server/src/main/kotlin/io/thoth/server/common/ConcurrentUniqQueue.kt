package io.thoth.server.common

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.milliseconds

class ConcurrentUniqQueue<T>(
    private val covers: (wider: T, narrower: T) -> Boolean = { _, _ -> false },
) {
    private val lock = ReentrantLock()
    private val notEmpty = lock.newCondition()
    private val entries = ArrayDeque<T>()

    val size: Int
        get() = lock.withLock { entries.size }

    fun isEmpty(): Boolean = lock.withLock { entries.isEmpty() }

    fun add(item: T) {
        lock.withLock {
            // Only the wider entry may evict the narrower one: it covers every descendant, while a
            // descendant would drop the rest of the wider entry's subtree.
            val covering = entries.firstOrNull { covers(it, item) }
            if (covering != null) {
                entries.remove(covering)
                entries.addLast(covering)
            } else {
                entries.removeAll { it == item || covers(item, it) }
                entries.addLast(item)
            }
            notEmpty.signalAll()
        }
    }

    fun pop(): T? = lock.withLock { entries.removeFirstOrNull() }

    fun poll(): T? =
        lock.withLock {
            var remaining = 10.milliseconds.inWholeNanoseconds
            while (entries.isEmpty() && remaining > 0) remaining = notEmpty.awaitNanos(remaining)
            entries.removeFirstOrNull()
        }

    fun removeAll(predicate: (T) -> Boolean) = lock.withLock { entries.removeAll(predicate) }

    fun drain(): List<T> = lock.withLock { entries.toList().also { entries.clear() } }
}
