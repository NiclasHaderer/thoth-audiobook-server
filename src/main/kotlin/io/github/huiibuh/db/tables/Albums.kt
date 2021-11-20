package io.github.huiibuh.db.tables

import io.github.huiibuh.models.AlbumModel
import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*


object Albums : UUIDTable() {
    val title = varchar("title", 255)
    val language = varchar("language", 255).nullable()
    val description = text("description").nullable()
    val artist = reference("artist", Artists)
    val asin = char("asin", 10).uniqueIndex().nullable()
    val composer = reference("composer", Artists).nullable()
    val collection = reference("collection", Collections).nullable()
    val collectionIndex = integer("collectionIndex").nullable()
    val cover = reference("cover", Images).nullable()
}

class Album(id: EntityID<UUID>) : UUIDEntity(id), ToModel<AlbumModel> {
    companion object : UUIDEntityClass<Album>(Albums)

    private val artistID by Albums.artist
    private val composerID by Albums.composer
    private val collectionID by Albums.collection
    private val coverID by Albums.cover

    var title by Albums.title
    var language by Albums.language
    val description by Albums.description
    val asin by Albums.asin
    var artist by Artist referencedOn Albums.artist
    var composer by Artist optionalReferencedOn Albums.composer
    var collection by Collection optionalReferencedOn Albums.collection
    var collectionIndex by Albums.collectionIndex
    var cover by Image optionalReferencedOn Albums.cover

    override fun toModel() =
        AlbumModel(id.value,
                   title,
                   language,
                   description,
                   asin,
                   artistID.value,
                   composerID?.value,
                   collectionID?.value,
                   collectionIndex,
                   coverID?.value)
}
