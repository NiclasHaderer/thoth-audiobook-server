package io.github.huiibuh.scanner

import java.io.File

val AUDIO_EXTENSIONS = setOf("mp3", "flac", "ogg", "vobis", "mp4", "m4a", "m4p", "m4b", "aiff", "wav", "wma", "dsf")

fun File.isAudioFile(): Boolean {
    return this.isFile && this.extension.lowercase() in AUDIO_EXTENSIONS
}

fun getAllAudioFiles(directory: String): List<File> {
    val file = File(directory)
    return file.walkTopDown().filter { f -> f.isAudioFile() }.toList()
}
