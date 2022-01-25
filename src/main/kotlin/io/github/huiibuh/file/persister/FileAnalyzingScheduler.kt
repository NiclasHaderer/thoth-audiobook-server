package io.github.huiibuh.file.persister

import io.github.huiibuh.extensions.classLogger
import io.github.huiibuh.file.analyzer.AudioFileAnalyzerWrapper
import io.github.huiibuh.file.scanner.CompleteScan
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.LinkedBlockingQueue
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
    private val metadataManager = TrackManagerImpl()
    private val fileQueue = LinkedBlockingQueue<Path>()
    private val removeItem = LinkedBlockingQueue<Path>()

    init {
        GlobalScope.launch(context = Dispatchers.IO) {
            launch { listenForNewFiles() }
            launch { listenForRemovePath() }
        }
    }

    private suspend fun CoroutineScope.listenForNewFiles() {
        while (true) {
            val item = fileQueue.take()
            extractAndSaveMetadata(item)
        }
    }

    private fun CoroutineScope.listenForRemovePath() {
        while (true) {
            val item = removeItem.take()
            removePath(item)
        }
    }

    private suspend fun extractAndSaveMetadata(path: Path) {
        val attrs = withContext(Dispatchers.IO) { Files.readAttributes(path, BasicFileAttributes::class.java) }
        val result = analyzer.analyze(path, attrs)
            ?: return log.warn("Skipped ${path.absolute()} because it contains not enough information")
        metadataManager.insertScanResult(result, path)
    }

    private fun removePath(path: Path) = metadataManager.removePath(path)


    override fun queue(type: FileAnalyzingScheduler.Type, path: Path) {
        when (type) {
            FileAnalyzingScheduler.Type.ADD_FILE -> {
                if (path.isDirectory()) {
                    log.warn("You tried to queue a folder for a metadata scan. This can not be done")
                    log.warn("The folder ${path.fileName} will therefore be skipped")
                } else {
                    fileQueue.add(path)
                }
            }
            FileAnalyzingScheduler.Type.REMOVE_FILE -> removeItem.add(path)
            FileAnalyzingScheduler.Type.SCAN_FOLDER -> CompleteScan(path).start()
        }
    }

}
