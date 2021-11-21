package io.github.huiibuh.db.tables

import io.github.huiibuh.models.ImageModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object TImages : UUIDTable("Images") {
    val image = blob("image").uniqueIndex()
}


class Image(id: EntityID<UUID>) : UUIDEntity(id), ToModel<ImageModel> {
    companion object : UUIDEntityClass<Image>(TImages)

    var image by TImages.image

    override fun toModel() = ImageModel(
        id = id.value,
        image = image.bytes
    )
}
