package io.thoth.models

import java.util.*

data class ImageModel(
    val id: UUID,
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
