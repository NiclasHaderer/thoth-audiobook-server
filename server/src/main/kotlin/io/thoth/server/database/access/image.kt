package io.thoth.server.database.access

import io.thoth.server.database.tables.ImageTable
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import java.util.UUID

context(_: Transaction)
private fun createImage(imageBytes: ByteArray): UUID =
    ImageTable.insertAndGetId { it[blob] = ExposedBlob(imageBytes) }.value

context(_: Transaction)
fun getOrCreateImage(
    newImage: ByteArray?,
    currentImageID: UUID?,
): UUID? {
    if (newImage == null) return currentImageID
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
