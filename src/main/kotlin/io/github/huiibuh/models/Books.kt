package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*


@Serializable
data class BookModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val title: String,
    val language: String?,
    val description: String?,
    val asin: String?,
    @Serializable(UUIDSerializer::class) val author: UUID,
    @Serializable(UUIDSerializer::class) val narrator: UUID?,
    @Serializable(UUIDSerializer::class) val series: UUID?,
    val seriesIndex: Int?,
    @Serializable(UUIDSerializer::class) val cover: UUID?,
)

@Serializable
data class BookWithTracks(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val title: String,
    val language: String?,
    val description: String?,
    val asin: String?,
    @Serializable(UUIDSerializer::class) val author: UUID,
    @Serializable(UUIDSerializer::class) val narrator: UUID?,
    @Serializable(UUIDSerializer::class) val series: UUID?,
    val seriesIndex: Int?,
    @Serializable(UUIDSerializer::class) val cover: UUID?,
)
