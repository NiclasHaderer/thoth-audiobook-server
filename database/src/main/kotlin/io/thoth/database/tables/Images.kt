package io.thoth.database.tables

import io.thoth.common.extensions.uriToFile
import io.thoth.database.ToModel
import io.thoth.models.ImageModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object TImages : UUIDTable("Images") {
    val blob = blob("image")
}


class Image(id: EntityID<UUID>) : UUIDEntity(id), ToModel<ImageModel> {
    companion object : UUIDEntityClass<Image>(TImages) {
        fun getMultiple(limit: Int, offset: Long) = transaction {
            TImages.slice(TImages.id).selectAll().limit(limit, offset * limit).map {
                it[TImages.id]
            }.map { it.value }
        }


        suspend fun create(string: String): Image {
            val imageBytes = string.uriToFile()
            return create(imageBytes)
        }

        fun create(imageBytes: ByteArray) = transaction {
            Image.new {
                blob = ExposedBlob(imageBytes)
            }
        }

        fun getById(uuid: UUID) = transaction {
            findById(uuid)?.toModel()
        }
    }

    var blob by TImages.blob

    override fun toModel() = ImageModel(
        id = id.value,
        blob = blob.bytes
    )
}
