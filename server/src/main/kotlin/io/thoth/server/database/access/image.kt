package io.thoth.server.database.access

import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.extensions.isUUID
import io.thoth.server.common.extensions.syncUriToFile
import io.thoth.server.database.tables.Image
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import java.util.*

fun Image.Companion.create(imageBytes: ByteArray): Image = new { blob = ExposedBlob(imageBytes) }

fun Image.areSame(newImageBytes: ByteArray): Boolean = blob.bytes.contentEquals(newImageBytes)

fun Image.Companion.getNewImage(
    newImage: String?,
    currentImageID: EntityID<UUID>?,
    default: EntityID<UUID>?,
): EntityID<UUID>? {
    if (newImage == null) return default

    if (newImage.isUUID()) {
        val newImageUUID = UUID.fromString(newImage)

        return findById(newImageUUID)?.id ?: throw ErrorResponse.notFound("Image", newImageUUID)
    }

    val originalImage = if (currentImageID != null) findById(currentImageID) else null
    val newImageBytes = newImage.syncUriToFile()
    val areSameImage = originalImage?.areSame(newImageBytes) ?: false
    return if (areSameImage) {
        currentImageID
    } else {
        create(newImageBytes).id
    }
}
