package io.github.huiibuh.file.scanner

import io.github.huiibuh.db.removeAllUnusedFromDb
import io.github.huiibuh.services.Scanner
import io.github.huiibuh.settings.Settings
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import io.methvin.watcher.hashing.FileHasher
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.name


class UpdateService() : KoinComponent {
    val settings by inject<Settings>()
    private val watcher =
        DirectoryWatcher.builder().path(Path.of(settings.audioFileLocation)).listener { event: DirectoryChangeEvent ->
            val path = event.path() // Ignore if it is a directory or not an audio file
            val eventType = event.eventType()

            if (event.isDirectory || !path.hasAudioExtension()) {

                val ignoreFile = settings.ignoreFile
                val fileName = path.name
                if (fileName == ignoreFile) {
                    Scanner.scanFolderForTracks(path.parent)
                    if (eventType == DirectoryChangeEvent.EventType.CREATE) {
                        removeAllUnusedFromDb()
                    }
                }

                return@listener
            }

            if (eventType == DirectoryChangeEvent.EventType.DELETE) {
                Scanner.fileDeleted(path.absolute().normalize())
            } else if (eventType == DirectoryChangeEvent.EventType.CREATE) {
                Scanner.fileCreated(path.absolute().normalize())
            }

        }.fileHasher(FileHasher.LAST_MODIFIED_TIME).build()

    @Throws(IOException::class)
    fun stopWatching() {
        watcher.close()
    }

    fun watch() {
        return watcher.watch()
    }
}
