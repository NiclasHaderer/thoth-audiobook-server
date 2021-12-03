package io.github.huiibuh.scanner

import io.github.huiibuh.config.Settings
import io.github.huiibuh.db.findOne
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.services.database.SharedSettingsService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolutePathString
import kotlin.io.path.getLastModifiedTime

class AudioFileVisitor(
    private val add: suspend (TrackReference, BasicFileAttributes, Path, Track?) -> Unit,
    private val removeSubtree: (Path) -> Unit,
) : FileVisitor<Path> {
    private val settings = SharedSettingsService.get()
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes?): FileVisitResult {
        val ignoreFile =
            Paths.get("${dir.absolutePathString()}${FileSystems.getDefault().separator}${Settings.ignoreFile}")
        return if (!Files.exists(ignoreFile)) {
            FileVisitResult.CONTINUE
        } else {
            removeSubtree(dir)
            FileVisitResult.SKIP_SUBTREE
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (file.isAudioFile()) {
            //            GlobalScope.launch {
            runBlocking {
                handleFile(file, attrs)
            }
            //            }
        }
        return FileVisitResult.CONTINUE
    }


    private suspend fun handleFile(file: Path, attrs: BasicFileAttributes) {
        // Get the db track
        val dbTrack = transaction { Track.findOne { TTracks.path eq file.absolutePathString() } }

        // Check if the track has not been updated and if yes, skip it
        if (dbTrack != null && dbTrack.accessTime >= file.getLastModifiedTime().toMillis()) {
            transaction { dbTrack.scanIndex = settings.scanIndex + 1 }
            return
        }
        sendToUpdateCallback(file, attrs, dbTrack)
    }

    private suspend fun sendToUpdateCallback(file: Path, attrs: BasicFileAttributes, dbTrack: Track?) {
        // Check if the file has all required attributes
        val audioFile = TrackReference.fromPath(file.absolutePathString())
        if (audioFile.hasRequiredAttributes()) {
            add(audioFile, attrs, file, dbTrack)
        } else {
            // Try to remove the file form the db if the file was indexed previously
            if (dbTrack != null) transaction { dbTrack.delete() }
            log.info("File ${file.absolutePathString()} is missing artist and album tags. ")
            log.info("The file will be ignored")
        }
    }


    override fun visitFileFailed(file: Path, exc: IOException?) = FileVisitResult.CONTINUE

    override fun postVisitDirectory(dir: Path, exc: IOException?) = FileVisitResult.CONTINUE
}
