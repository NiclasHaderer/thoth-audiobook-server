package io.github.huiibuh.db.tables

import io.github.huiibuh.models.CollectionModel
import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Collections : UUIDTable() {
    val title = varchar("title", 250).uniqueIndex()
    val asin = char("asin", 10).uniqueIndex().nullable()
    val description = text("description").nullable()
    val artist = reference("artist", Artists)
}

class Collection(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Collection>(Collections)

    private val artistID by Collections.artist

    var title by Collections.title
    var asin by Collections.asin
    var description by Collections.description
    var artist by Artist referencedOn Collections.artist

    fun toModel() = CollectionModel(id.value, title, asin, description, artistID.value)
}
