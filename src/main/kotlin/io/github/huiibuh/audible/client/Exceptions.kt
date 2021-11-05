package io.github.huiibuh.audible.client

import kotlinx.serialization.Serializable

@Serializable
data class AudibleError(
    val audibleStatus: Int,
    val message: String,
)

abstract class AudibleBaseException(
    message: String,
    private val statusCode: Int,
) : Exception(message) {
    fun toModel() = AudibleError(this.statusCode, this.message!!)
}

class AudibleNotFoundException(message: String, statusCode: Int) : AudibleBaseException(message, statusCode)
class AudibleException(message: String, statusCode: Int) : AudibleBaseException(message, statusCode)
