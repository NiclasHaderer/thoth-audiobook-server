package io.github.huiibuh.db.tables

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
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


class Artist(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Artist>(Artists)

    private val imageID by Artists.image

    var name by Artists.name
    var biography by Artists.biography
    var asin by Artists.asin
    var image by Image optionalReferencedOn Artists.image

    fun toModel() = ArtistModel(id.value, name, biography, asin, imageID?.value)
}

@Serializable
data class ArtistModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val name: String,
    val biography: String?,
    val asin: String?,
    @Serializable(UUIDSerializer::class) val image: UUID?,
)
