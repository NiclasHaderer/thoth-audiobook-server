package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

@Serializable
data class AuthorModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val name: String,
    val biography: String?,
    val asin: String?,
    @Serializable(UUIDSerializer::class) val image: UUID?,
)
