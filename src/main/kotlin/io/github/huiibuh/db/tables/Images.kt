package io.github.huiibuh.db.tables

import io.github.huiibuh.models.ImageModel
import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Images : UUIDTable() {
    val image = blob("image").uniqueIndex()
}


class Image(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Image>(Images)

    var image by Images.image

    fun toModel() = ImageModel(id.value, image.bytes)
}
