package io.thoth.server.database.access

import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.extensions.isUUID
import io.thoth.server.common.extensions.syncUriToFile
import io.thoth.server.database.tables.ImageEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import java.util.UUID

fun ImageEntity.Companion.create(imageBytes: ByteArray): ImageEntity = new { blob = ExposedBlob(imageBytes) }

fun ImageEntity.areSame(newImageBytes: ByteArray): Boolean = blob.bytes.contentEquals(newImageBytes)

sealed interface NewImage {
    class Stored(
        val id: UUID,
    ) : NewImage

    class Downloaded(
        val bytes: ByteArray,
    ) : NewImage
}

fun fetchImage(image: String?): NewImage? =
    when {
        image == null -> null
        image.isUUID() -> NewImage.Stored(UUID.fromString(image))
        else -> NewImage.Downloaded(image.syncUriToFile())
    }

fun ImageEntity.Companion.getNewImage(
    newImage: NewImage?,
    currentImageID: EntityID<UUID>?,
    default: EntityID<UUID>?,
): EntityID<UUID>? =
    when (newImage) {
        null -> default
        is NewImage.Stored -> findById(newImage.id)?.id ?: throw ErrorResponse.notFound("Image", newImage.id)
        is NewImage.Downloaded -> {
            val originalImage = if (currentImageID != null) findById(currentImageID) else null
            if (originalImage?.areSame(newImage.bytes) == true) currentImageID else create(newImage.bytes).id
        }
    }
