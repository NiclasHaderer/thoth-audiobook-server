package io.thoth.database.access

import io.ktor.http.*
import io.thoth.common.extensions.isUUID
import io.thoth.common.extensions.syncUriToFile
import io.thoth.database.tables.Image
import io.thoth.models.ImageModel
import io.thoth.openapi.ErrorResponse
import java.util.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.statements.api.ExposedBlob

fun Image.Companion.create(imageBytes: ByteArray): Image {
    return Image.new { blob = ExposedBlob(imageBytes) }
}

fun Image.Companion.getById(uuid: UUID): ImageModel? {
    return Image.findById(uuid)?.toModel()
}

fun Image.toModel() = ImageModel(id = id.value, blob = blob.bytes)

fun Image.areSame(newImageBytes: ByteArray): Boolean {
    return blob.bytes.contentEquals(newImageBytes)
}

fun Image.Companion.getNewImage(
    newImage: String?,
    currentImageID: EntityID<UUID>?,
    default: EntityID<UUID>?
): EntityID<UUID>? {

    if (newImage == null) return default

    if (newImage.isUUID()) {
        val newImageUUID = UUID.fromString(newImage)

        return Image.findById(newImageUUID)?.id
            ?: throw ErrorResponse(HttpStatusCode.BadRequest, "Image with id $newImageUUID does not exist")
    }

    val originalImage = if (currentImageID != null) Image.findById(currentImageID) else null
    val newImageBytes = newImage.syncUriToFile()
    val areSameImage = originalImage?.areSame(newImageBytes) ?: false
    return if (areSameImage) {
        currentImageID
    } else {
        create(newImageBytes).id
    }
}
