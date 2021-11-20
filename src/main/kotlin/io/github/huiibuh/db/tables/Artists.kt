package io.github.huiibuh.db.tables

import io.github.huiibuh.models.ArtistModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Artists : UUIDTable() {
    val name = varchar("name", 255).uniqueIndex()
    val biography = text("biography").nullable()
    val asin = text("asin").uniqueIndex().nullable()
    val image = reference("image", Images).nullable()
}


class Artist(id: EntityID<UUID>) : UUIDEntity(id), ToModel<ArtistModel> {
    companion object : UUIDEntityClass<Artist>(Artists)

    private val imageID by Artists.image

    var name by Artists.name
    var biography by Artists.biography
    var asin by Artists.asin
    var image by Image optionalReferencedOn Artists.image

    override fun toModel() = ArtistModel(id.value, name, biography, asin, imageID?.value)
}
