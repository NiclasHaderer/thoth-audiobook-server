import io.ktor.http.*

class RequestMetadata<T: RequestBodySetter>(
    val path: String,
    val method: HttpMethod,
    val headers: Headers,
    val body: T,
    val shouldLogin: Boolean,
    val securitySchema: String?
)
