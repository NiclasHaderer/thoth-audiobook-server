package io.thoth.server.file.scanner

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import io.methvin.watcher.hashing.FileHasher
import io.thoth.common.extensions.hasAudioExtension
import io.thoth.config.ThothConfig
import io.thoth.server.file.persister.AudioAnalyzer
import java.nio.file.Path
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface FileWatcher {
    fun watch()
    fun stop()
}

class FileWatcherImpl : FileWatcher, KoinComponent {
    private val thothConfig: ThothConfig by inject()
    private val audioAnalyzer: AudioAnalyzer by inject()

    private val watcher =
        DirectoryWatcher.builder()
            .paths(thothConfig.audioFileLocations.map { Path.of(it) })
            .listener(this::contentChanged)
            .fileHasher(FileHasher.LAST_MODIFIED_TIME)
            .build()

    private fun contentChanged(event: DirectoryChangeEvent) {
        val path = event.path() // Ignore if it is a directory or not an audio file
        val eventType = event.eventType()

        TODO("Check if the file is below a .thothignore file")

        if (event.isDirectory || !path.hasAudioExtension()) {
            TODO("Recursive scan directory")
            TODO("Check if the directory is below a .thothignore file")
        }

        if (eventType == DirectoryChangeEvent.EventType.DELETE) {
            audioAnalyzer.queue(AudioAnalyzer.Type.REMOVE_FILE, path)
        } else if (eventType == DirectoryChangeEvent.EventType.CREATE) {
            audioAnalyzer.queue(AudioAnalyzer.Type.ADD_FILE, path)
        }
    }

    override fun watch() {
        return watcher.watch()
    }

    override fun stop() {
        return watcher.close()
    }
}
