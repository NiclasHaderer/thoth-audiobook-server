package io.github.huiibuh.file.persister

import io.github.huiibuh.extensions.classLogger
import io.github.huiibuh.file.analyzer.AudioFileAnalyzerWrapper
import io.github.huiibuh.file.scanner.CompleteScan
import io.github.huiibuh.settings.Settings
import io.github.huiibuh.utils.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory

interface FileAnalyzingScheduler {
    fun queue(type: Type, path: Path)

    enum class Type {
        REMOVE_FILE, ADD_FILE, SCAN_FOLDER
    }
}

@OptIn(DelicateCoroutinesApi::class)
class FileAnalyzingSchedulerImpl : KoinComponent, FileAnalyzingScheduler {
    private val log = classLogger()
    private val analyzer by inject<AudioFileAnalyzerWrapper>()
    private val settings by inject<Settings>()
    private val scanScheduler = Scheduler(1)
    private val trackManager = TrackManagerImpl()
    private val fileQueue = Channel<Path>(settings.analyzerThreads)
    private val removeItem = Channel<Path>(settings.analyzerThreads)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Launch two coroutines which will do nothing else, but listen for new tasks in the channels and
        // execute the queued tasks
        scope.launch {
            launch { listenForNewFiles() }
            launch { listenForRemovePath() }
        }
    }

    private suspend fun listenForNewFiles() {
        while (true) {
            val item = fileQueue.receive()
            extractAndSaveMetadata(item)
        }
    }

    private suspend fun listenForRemovePath() {
        while (true) {
            val item = removeItem.receive()
            removePath(item)
        }
    }

    private suspend fun extractAndSaveMetadata(path: Path) {
        scanScheduler.queue {
            val attrs = withContext(Dispatchers.IO) { Files.readAttributes(path, BasicFileAttributes::class.java) }
            val result = analyzer.analyze(path, attrs)
                ?: return@queue log.warn("Skipped ${path.absolute()} because it contains not enough information")
            trackManager.insertScanResult(result, path)
        }

    }

    private fun removePath(path: Path) = trackManager.removePath(path)

    /**
     * @param path The path you want to queue for a scan
     * @param type Do you want to queue a complete scan, remove a file or add a file
     */
    override fun queue(type: FileAnalyzingScheduler.Type, path: Path) {
        runWithoutBlocking {
            when (type) {
                FileAnalyzingScheduler.Type.ADD_FILE -> {
                    if (path.isDirectory()) {
                        log.warn("You tried to queue a folder for a metadata scan. This can not be done")
                        log.warn("The folder ${path.fileName} will therefore be skipped")
                    } else {
                        fileQueue.send(path)
                    }
                }
                FileAnalyzingScheduler.Type.REMOVE_FILE -> removeItem.send(path)
                FileAnalyzingScheduler.Type.SCAN_FOLDER -> CompleteScan(path).start()
            }
        }
    }

    private fun runWithoutBlocking(callback: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            launch {
                callback()
            }
        }
    }
}
