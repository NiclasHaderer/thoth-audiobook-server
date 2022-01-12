package io.github.huiibuh.db.tables

import io.github.huiibuh.api.exceptions.APINotFound
import io.github.huiibuh.db.ToModel
import io.github.huiibuh.models.ImageModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
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
