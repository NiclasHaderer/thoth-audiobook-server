package io.thoth.server.file.scanner

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import io.methvin.watcher.hashing.FileHasher
import io.thoth.server.common.extensions.hasAudioExtension
import io.thoth.server.config.ThothConfig
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface FileTreeWatcher {
    fun watch(folders: List<Path>)

    fun stop()
}

class FileTreeWatcherImpl : FileTreeWatcher, KoinComponent {
    private val thothConfig by inject<ThothConfig>()
    private val libraryScanner by inject<LibraryScanner>()

    private var watcher: DirectoryWatcher? = null

    private fun contentChanged(event: DirectoryChangeEvent) {
        val path = event.path() // Ignore if it is a directory or not an audio file
        val eventType = event.eventType()

        if (!path.isRegularFile() && path.name == thothConfig.ignoreFile) {
            if (eventType == DirectoryChangeEvent.EventType.CREATE) {
                libraryScanner.ignoreFolder(path.parent)
            } else if (eventType == DirectoryChangeEvent.EventType.DELETE) {
                libraryScanner.unIgnoreFolder(path.parent)
            }
            return
        }

        if (event.isDirectory) {
            libraryScanner.scanFolder(path, null)
        }

        if (eventType == DirectoryChangeEvent.EventType.DELETE) {
            libraryScanner.removePath(path)
        } else if (eventType == DirectoryChangeEvent.EventType.CREATE && path.hasAudioExtension()) {
            runBlocking { libraryScanner.addOrUpdatePath(path, null) }
        }
    }

    override fun watch(folders: List<Path>) {
        if (watcher != null) {
            stop()
        }

        watcher =
            DirectoryWatcher.builder()
                .paths(folders)
                .listener(this::contentChanged)
                .fileHasher(FileHasher.LAST_MODIFIED_TIME)
                .build()
                .also { it.watch() }
    }

    override fun stop() {
        watcher?.close()
        watcher = null
    }
}
