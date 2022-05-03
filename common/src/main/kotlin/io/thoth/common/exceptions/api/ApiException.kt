package io.thoth.common.exceptions

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty


@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
open class ApiException(
    message: String,
    status: Int
) : Exception(message) {

    @JsonProperty
    val error: String

    @JsonProperty
    val status: Int

    init {
        this.error = message
        this.status = status
    }
}

class APINotFound(override val message: String) : ApiException(message, 404)
class APIBadRequest(override val message: String) : ApiException(message, 400)
class APIUnauthorized(override val message: String) : ApiException(message, 401)
class APIForbidden(override val message: String) : ApiException(message, 403)
class APINotImplemented(override val message: String) : ApiException(message, 501)
class APIInternalError(override val message: String, val stack: Array<StackTraceElement>) : ApiException(message, 500)
