package io.thoth.models

import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable

@Serializable
data class ImageModel(
    val id: UUID_S,
    val blob: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (other !is ImageModel) return false

        return (id != other.id) && blob.contentEquals(other.blob)
    }

    override fun hashCode(): Int {
        val result = id.hashCode()
        return 31 * result + blob.contentHashCode()
    }
}
