package io.github.huiibuh.api.exceptions

data class APIError(
    val error: String,
)

open class ApiException(message: String, val status: Int) : Exception(message) {
    fun toModel() = APIError(this.message!!)
}

class APINotFound(override val message: String) : ApiException(message, 404)
class APIBadRequest(override val message: String) : ApiException(message, 400)
class APIUnauthorized(override val message: String) : ApiException(message, 401)
class APIForbidden(override val message: String) : ApiException(message, 403)
class APINotImplemented(override val message: String) : ApiException(message, 501)
