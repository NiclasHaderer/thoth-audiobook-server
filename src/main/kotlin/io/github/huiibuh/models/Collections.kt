package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

@Serializable
data class CollectionModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val name: String,
    val asin: String?,
    val description: String?,
    @Serializable(UUIDSerializer::class) val artist: UUID,
)
