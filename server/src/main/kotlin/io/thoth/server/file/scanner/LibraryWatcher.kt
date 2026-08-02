package io.thoth.server.file.scanner

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import io.thoth.server.common.extensions.hasAudioExtension
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.file.TrackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.absolute
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

interface LibraryWatcher {
    fun start()

    fun restart()

    fun stop()
}

private enum class ChangeKind { UPSERT, REMOVE }

class LibraryWatcherImpl(
    private val debounce: Duration = 10.seconds,
) : LibraryWatcher,
    KoinComponent {
    private val log = logger {}
    private val scanner by inject<LibraryScanner>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val lock = Mutex()
    private var watcher: DirectoryWatcher? = null
    private var roots: List<WatchedLibrary> = emptyList()

    // Guarded by a plain lock, not the Mutex: the watcher calls onEvent on one thread, and recording the
    // change there rather than in a coroutine is what keeps a CREATE/DELETE pair in the order it happened.
    private val pendingLock = ReentrantLock()
    private val pending = mutableMapOf<Path, ChangeKind>()
    private var deadline = 0L
    private var worker: Job? = null

    private data class WatchedLibrary(
        val id: UUID,
        val folders: List<Path>,
    )

    override fun start() {
        scope.launch { startWatching() }
    }

    private suspend fun startWatching() = lock.withLock { openWatcher() }

    private fun openWatcher() {
        val libraries =
            transaction {
                LibraryEntity.all().map { library ->
                    WatchedLibrary(library.id.value, library.folders.map { libraryRoot(it) })
                }
            }
        val folders = libraries.flatMap { it.folders }.filter { it.toFile().isDirectory }
        roots = libraries
        if (folders.isEmpty()) {
            log.info { "No library folders to watch" }
            return
        }

        watcher =
            DirectoryWatcher
                .builder()
                .paths(folders)
                // Hashing costs a full-tree stat sweep on boot and a resident hash per file, and only
                // buys duplicate suppression, which shouldUpdate already does from the database.
                .fileHashing(false)
                .fileTreeVisitor(IgnoreAwareVisitor)
                .listener(::onEvent)
                .build()
                .also { it.watchAsync() }
        log.info { "Watching ${folders.size} library folder(s)" }
    }

    private fun closeWatcher() {
        watcher?.close()
        watcher = null
    }

    // Both halves under one lock, or two interleaved restarts leave a watcher open that nothing can close
    override fun restart() {
        scope.launch {
            lock.withLock {
                closeWatcher()
                openWatcher()
            }
        }
    }

    // Blocks: the watcher owns threads that would otherwise keep the JVM from exiting
    override fun stop() = runBlocking { stopWatching() }

    private suspend fun stopWatching() {
        lock.withLock { closeWatcher() }
        worker?.cancelAndJoin()
        worker = null
    }

    internal fun onEvent(event: DirectoryChangeEvent) {
        // Overflow means the OS dropped events and carries no path, so the whole root has to be re-walked
        if (event.eventType() == DirectoryChangeEvent.EventType.OVERFLOW) {
            val root = event.rootPath()?.absolute() ?: return
            log.warn { "Watch events were dropped, rescanning '$root'" }
            queue(root, ChangeKind.UPSERT)
            return
        }

        val path = event.path()?.absolute() ?: return
        val remove = event.eventType() == DirectoryChangeEvent.EventType.DELETE

        if (path.name == IGNORE_FILE) {
            val folder = path.parent ?: return
            // Inverted: the marker appearing means the folder has to go, and it disappearing brings it back
            queue(folder, if (remove) ChangeKind.UPSERT else ChangeKind.REMOVE)
            return
        }

        val root = rootOf(path)
        if (root == null || (!remove && isIgnored(path, root))) return

        if (event.isDirectory) {
            queue(path, if (remove) ChangeKind.REMOVE else ChangeKind.UPSERT)
            return
        }

        if (!path.hasAudioExtension()) return
        queue(path, if (remove) ChangeKind.REMOVE else ChangeKind.UPSERT)
    }

    private fun queue(
        path: Path,
        kind: ChangeKind,
    ) {
        pendingLock.withLock {
            pending[path] = kind
            deadline = System.nanoTime() + debounce.inWholeNanoseconds
            // One worker for the lifetime of a burst, so stop() always has something to join. Cancelling
            // and replacing it would leave an in-flight apply() untracked.
            if (worker?.isActive != true) worker = scope.launch { drain() }
        }
    }

    private suspend fun drain() {
        while (true) {
            val wait = pendingLock.withLock { deadline - System.nanoTime() }
            if (wait > 0) {
                delay(wait.nanoseconds)
                continue
            }
            val batch =
                pendingLock.withLock {
                    if (pending.isEmpty()) return
                    pending.toMap().also { pending.clear() }
                }
            apply(batch)
        }
    }

    private fun apply(batch: Map<Path, ChangeKind>) {
        val byLibrary = batch.entries.groupBy { libraryOf(it.key) }
        for ((libraryId, changes) in byLibrary) {
            if (libraryId == null) continue
            val library = transaction { LibraryEntity.findById(libraryId) } ?: continue
            log.info { "Applying ${changes.size} change(s) to library ${library.name}" }

            val removed = changes.filter { it.value == ChangeKind.REMOVE }.map { it.key }
            // Removals first, so an event that arrived just after its folder was pruned does not
            // resurrect the file the removal was meant to take with it
            for (path in removed) {
                runCatching { TrackManager.removeFolder(path, library) }
                    .onFailure { log.warn(it) { "Could not remove '$path'" } }
            }

            for (path in changes.filter { it.value == ChangeKind.UPSERT }.map { it.key }) {
                if (removed.any { path.startsWith(it) }) continue
                runCatching {
                    if (path.toFile().isDirectory) {
                        scanner.scanFolder(path, library)
                    } else if (scanner.shouldUpdate(path)) {
                        TrackManager.addPath(path, library)
                    }
                }.onFailure { log.warn(it) { "Could not apply change to '$path'" } }
            }

            if (removed.isNotEmpty()) {
                scanner.cleanupLibrary(library)
            }
        }
    }

    private fun libraryOf(path: Path): UUID? =
        roots.firstOrNull { library -> library.folders.any { path.startsWith(it) } }?.id

    private fun rootOf(path: Path): Path? =
        roots.firstNotNullOfOrNull { library -> library.folders.firstOrNull { path.startsWith(it) } }
}
