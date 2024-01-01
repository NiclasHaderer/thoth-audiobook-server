import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.date.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlin.coroutines.CoroutineContext


class OpenApiHttpResponse<T>(private val delegate: HttpResponse, val typeInfo: TypeInfo) : HttpResponse() {
    override val call: HttpClientCall
        get() = delegate.call

    @io.ktor.util.InternalAPI
    override val content: ByteReadChannel
        get() = delegate.content
    override val requestTime: GMTDate
        get() = delegate.requestTime
    override val responseTime: GMTDate
        get() = delegate.responseTime
    override val status: HttpStatusCode
        get() = delegate.status
    override val version: HttpProtocolVersion
        get() = delegate.version
    override val headers: Headers
        get() = delegate.headers
    override val coroutineContext: CoroutineContext
        get() = delegate.coroutineContext

    @Suppress("UNCHECKED_CAST")
    suspend inline fun body(): T = call.body(typeInfo) as T
}
