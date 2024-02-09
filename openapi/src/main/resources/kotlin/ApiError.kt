enum class ApiErrorType {
    HTTP, Connection, ClientSerialization, Unknown,
}

open class InternalApiError(
    val status: Int,
    val error: String,
    val details: String? = null,
    val body: String? = null,
)

class ApiError(
    val type: ApiErrorType,
    status: Int,
    error: String,
    details: String? = null,
    body: String? = null,
) : InternalApiError(status, error, details, body) {
    constructor(type: ApiErrorType, error: InternalApiError) : this(
        type,
        error.status,
        error.error,
        error.details,
        error.body,
    )

    override fun toString(): String {
        return "ApiError(type=$type, status=$status, error=$error, details=$details, body=$body)"
    }
}


fun InternalApiError.wrap(type: ApiErrorType): ApiError {
    return ApiError(type, this)
}

