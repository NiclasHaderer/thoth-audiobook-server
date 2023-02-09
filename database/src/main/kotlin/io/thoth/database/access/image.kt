package io.thoth.database.access

import io.thoth.common.extensions.syncUriToFile
import io.thoth.database.tables.Image
import io.thoth.models.ImageModel
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import java.util.*

fun Image.Companion.create(imageBytes: ByteArray): Image {
    return Image.new {
        blob = ExposedBlob(imageBytes)
    }
}

fun Image.Companion.getById(uuid: UUID): ImageModel? {
    return Image.findById(uuid)?.toModel()
}

fun Image.toModel() = ImageModel(
    id = id.value, blob = blob.bytes
)

fun Image.areSame(newImageBytes: ByteArray): Boolean {
    return blob.bytes.contentEquals(newImageBytes)
}


fun Image.Companion.getNewImage(
    newImage: String?, currentImageID: EntityID<UUID>?, default: EntityID<UUID>?
): EntityID<UUID>? {
    // TODO find a way to make this suspend
    return if (newImage != null) {
        val originalImage = if (currentImageID != null) Image.findById(currentImageID) else null
        val newImageBytes = newImage.syncUriToFile()
        val areSameImage = originalImage?.areSame(newImageBytes) ?: false
        if (areSameImage) {
            currentImageID
        } else {
            create(newImageBytes).id
        }
    } else {
        default
    }
}