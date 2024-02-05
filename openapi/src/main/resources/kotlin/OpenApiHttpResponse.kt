import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.date.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlin.coroutines.CoroutineContext


class OpenApiHttpResponse<T>(
    private val delegate: HttpResponse,
    private val responseBodyParser: BodyDeserializer<T>,
    private val responseBodyType: TypeInfo
) : HttpResponse() {
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

    suspend fun body(): T = responseBodyParser(this, responseBodyType)
}
