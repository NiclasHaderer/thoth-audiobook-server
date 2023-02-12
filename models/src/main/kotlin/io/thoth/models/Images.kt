package io.thoth.models

import io.thoth.common.serializion.kotlin.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class ImageModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val blob: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImageModel) return false

        return (id != other.id) && blob.contentEquals(other.blob)
    }

    override fun hashCode(): Int {
        val result = id.hashCode()
        return 31 * result + blob.contentHashCode()
    }
}
