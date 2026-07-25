package io.thoth.taglib

data class Picture(
    val data: ByteArray,
    val mimeType: String?,
    val description: String?,
    val type: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Picture) return false
        return data.contentEquals(other.data) &&
            mimeType == other.mimeType &&
            description == other.description &&
            type == other.type
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        return result
    }
}
