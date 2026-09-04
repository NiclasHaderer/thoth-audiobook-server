package io.thoth.server.database.access

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.server.common.imageContentType
import io.thoth.server.database.tables.ImageTable
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import java.util.UUID

private val log = logger {}

context(_: Transaction)
private fun createImage(imageBytes: ByteArray): UUID =
    ImageTable.insertAndGetId { it[blob] = ExposedBlob(imageBytes) }.value

context(_: Transaction)
fun getOrCreateImage(
    newImage: ByteArray?,
    currentImageID: UUID?,
): UUID? {
    if (newImage == null) return currentImageID
    // Cover art embedded in a track can be anything at all. Dropping it keeps a single odd file from failing
    // the whole import, which throwing here would do.
    if (imageContentType(newImage) == null) {
        log.warn { "Ignoring cover art that is not a supported image" }
        return currentImageID
    }
    val currentBytes =
        currentImageID?.let { id ->
            ImageTable
                .select(ImageTable.blob)
                .where { ImageTable.id eq id }
                .singleOrNull()
                ?.get(ImageTable.blob)
                ?.bytes
        }
    return if (currentBytes?.contentEquals(newImage) == true) currentImageID else createImage(newImage)
}
