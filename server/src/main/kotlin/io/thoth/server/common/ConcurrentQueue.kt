package io.thoth.server.common

import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ConcurrentQueue {
    private val lock = ReentrantLock()
    private val notEmpty = lock.newCondition()
    private val entries = ArrayDeque<Path>()
    private var closed = false

    val size: Int
        get() = lock.withLock { entries.size }

    fun isEmpty(): Boolean = lock.withLock { entries.isEmpty() }

    fun add(path: Path) {
        lock.withLock {
            // Overlap is component-wise, so "/lib/Dune 2" is not under "/lib/Dune". Only the wider entry may
            // evict the narrower one: a parent covers every child, but a child would drop the parent's subtree.
            val covering = entries.firstOrNull { it != path && path.startsWith(it) }
            if (covering != null) {
                entries.remove(covering)
                entries.addLast(covering)
            } else {
                entries.removeAll { it.startsWith(path) }
                entries.addLast(path)
            }
            // signalAll, not signal: an add can evict more entries than it inserts, so the number of
            // wakeups owed to consumers does not track the number of adds
            notEmpty.signalAll()
        }
    }

    fun pop(): Path? = lock.withLock { entries.removeFirstOrNull() }

    fun take(): Path? =
        lock.withLock {
            while (entries.isEmpty() && !closed) notEmpty.await()
            entries.removeFirstOrNull()
        }

    fun drain(): List<Path> = lock.withLock { entries.toList().also { entries.clear() } }

    fun close() {
        lock.withLock {
            closed = true
            notEmpty.signalAll()
        }
    }
}
