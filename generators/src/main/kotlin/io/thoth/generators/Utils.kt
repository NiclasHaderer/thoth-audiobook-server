package io.thoth.generators

import java.io.File

internal fun File.prependText(content: String) {
    val tempFile = File(this.absolutePath + ".tmp")
    tempFile.writeText(content)
    tempFile.appendBytes(this.readBytes())
    this.delete()
    tempFile.renameTo(this)
}
