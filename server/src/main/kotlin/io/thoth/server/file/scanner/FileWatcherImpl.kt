package io.thoth.server.file.scanner

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import io.methvin.watcher.hashing.FileHasher
import io.thoth.server.config.ThothConfig
import io.thoth.server.file.persister.FileAnalyzingScheduler
import java.nio.file.Path
import kotlin.io.path.name


interface FileWatcher {
    fun watch()
}

class FileWatcherImpl(
    private val thothConfig: ThothConfig,
    private val analyzer: FileAnalyzingScheduler
) : FileWatcher {

    private val watcher =
        DirectoryWatcher.builder()
            .paths(thothConfig.audioFileLocations.map { Path.of(it) })
            .listener { event: DirectoryChangeEvent ->
                val path = event.path() // Ignore if it is a directory or not an audio file
                val eventType = event.eventType()

                if (event.isDirectory || !path.hasAudioExtension()) {

                    val ignoreFile = thothConfig.ignoreFile
                    val fileName = path.name
                    if (fileName == ignoreFile) {
                        analyzer.queue(FileAnalyzingScheduler.Type.SCAN_FOLDER, path)
                    }

                    return@listener
                }

                if (eventType == DirectoryChangeEvent.EventType.DELETE) {
                    analyzer.queue(FileAnalyzingScheduler.Type.REMOVE_FILE, path)
                } else if (eventType == DirectoryChangeEvent.EventType.CREATE) {
                    analyzer.queue(FileAnalyzingScheduler.Type.ADD_FILE, path)
                }

            }.fileHasher(FileHasher.LAST_MODIFIED_TIME).build()

    override fun watch() {
        return watcher.watch()
    }
}
