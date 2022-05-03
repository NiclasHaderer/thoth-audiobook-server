package io.thoth.database.tables

import io.thoth.common.exceptions.APINotFound
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
    val image = blob("image")
}


class Image(id: EntityID<UUID>) : UUIDEntity(id), ToModel<ImageModel> {
    companion object : UUIDEntityClass<Image>(TImages) {
        fun removeUnused() = transaction {
            all().forEach {
                val imageID = it.id.value
                if (
                    Author.find { TAuthors.image eq imageID }.empty() &&
                    Book.find { TBooks.cover eq imageID }.empty()
                ) {
                    it.delete()
                }
            }
        }

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
                image = ExposedBlob(imageBytes)
            }
        }

        @Throws(APINotFound::class)
        fun getById(uuid: UUID) = transaction {
            findById(uuid)?.toModel() ?: throw APINotFound("Could not find image")
        }
    }

    var image by TImages.image

    override fun toModel() = ImageModel(
        id = id.value,
        image = image.bytes
    )
}
