package io.thoth.database.access

import io.thoth.common.extensions.uriToFile
import io.thoth.database.tables.Image
import io.thoth.database.tables.TImages
import io.thoth.models.ImageModel
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import java.util.*

fun Image.Companion.getMultiple(limit: Int, offset: Long): List<UUID> {
    return TImages.slice(TImages.id).selectAll().limit(limit, offset * limit).map {
        it[TImages.id]
    }.map { it.value }
}

suspend fun Image.Companion.create(string: String): Image {
    val imageBytes = string.uriToFile()
    return create(imageBytes)
}

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