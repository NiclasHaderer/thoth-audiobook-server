package io.thoth.server.file.scanner

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import io.thoth.server.common.extensions.canonical
import io.thoth.server.common.extensions.hasAudioExtension
import io.thoth.server.common.scheduling.Scheduler
import io.thoth.server.schedules.ThothSchedules
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.absolute
import kotlin.io.path.name

// All three block. Registering means querying the libraries and walking their trees to build the watcher's
// index, so whether that happens in the background is the caller's decision, not this class's: boot launches
// it so readiness is not delayed, while a library mutation waits so no event can be missed afterwards.
interface LibraryWatcher {
    fun start()

    fun restart()

    fun stop()
}

class LibraryWatcherImpl :
    LibraryWatcher,
    KoinComponent {
    private val log = logger {}
    private val pipeline by inject<LibraryImportPipeline>()
    private val roots by inject<LibraryRoots>()
    private val scheduler by inject<Scheduler>()
    private val schedules by inject<ThothSchedules>()

    private val lock = ReentrantLock()
    private var watcher: DirectoryWatcher? = null

    override fun start() = lock.withLock { openWatcher() }

    private fun openWatcher() {
        val folders = roots.all().flatMap { it.folders }.filter { it.toFile().isDirectory }
        if (folders.isEmpty()) {
            log.info { "No library folders to watch" }
            return
        }

        watcher =
            DirectoryWatcher
                .builder()
                .paths(folders)
                // Hashing costs a full-tree stat sweep on boot and a resident hash per file, and only
                // buys duplicate suppression, which needsAnalysis already does from the database.
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
    override fun restart() =
        lock.withLock {
            closeWatcher()
            openWatcher()
        }

    override fun stop() = lock.withLock { closeWatcher() }

    // Nothing but classification: whether a path is ignored, gone, still being copied or already current is
    // decided by the worker that picks the item up, so there is exactly one place that knows those rules.
    internal fun onEvent(event: DirectoryChangeEvent) {
        if (event.eventType() == DirectoryChangeEvent.EventType.OVERFLOW) {
            // Overflow means the OS dropped an unknown set of events, deletions among them, and a subtree
            // walk can only find files that exist. Only a scan can reap what is already gone.
            val root = event.rootPath()?.canonical() ?: return
            val library = roots.owning(root) ?: return
            log.info { "Watch events were dropped, rescanning library '${library.name}'" }
            scheduler.dispatch(schedules.scanLibrary.build(library.id))
            return
        }

        val path = event.path()?.canonical() ?: return
        if (event.isDirectory || path.name == IGNORE_FILE || path.hasAudioExtension()) pipeline.enqueue(path)
    }
}
