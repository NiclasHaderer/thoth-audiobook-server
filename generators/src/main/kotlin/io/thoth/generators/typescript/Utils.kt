package io.thoth.generators.typescript

import java.io.File

internal fun File.prependText(content: String) {
    val tempFile = File(this.absolutePath + ".tmp")
    tempFile.writeText(content)
    tempFile.appendBytes(this.readBytes())
    this.delete()
    tempFile.renameTo(this)
}
