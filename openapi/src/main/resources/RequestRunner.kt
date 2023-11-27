import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.date.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlin.coroutines.CoroutineContext


interface RequestMetadata<T, RESPONSE> {
    val url: Url
    val method: HttpMethod
    val headers: Headers
    val body: T
    val shouldLogin: Boolean
    val securitySchema: String
}

typealias OnBeforeRequest<T, R> = (metadata: RequestMetadata<T, R>, requestBuilder: HttpRequestBuilder) -> Unit
typealias OnAfterRequest<T, R> = (metadata: RequestMetadata<T, R>, response: HttpResponse) -> Unit


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


open class RequestRunner(
    clientBuilder: HttpClientConfig<*>.() -> Unit = {}
) {
    private val beforeRequestHooks: MutableList<OnBeforeRequest<*, *>> = mutableListOf()
    private val afterRequestHooks: MutableList<OnAfterRequest<*, *>> = mutableListOf()
    private val client = HttpClient { clientBuilder() }

    fun onBeforeRequest(onBeforeRequest: OnBeforeRequest<*, *>) {
        beforeRequestHooks.add(onBeforeRequest)
    }

    fun onAfterRequest(onAfterRequest: OnAfterRequest<*, *>) {
        afterRequestHooks.add(onAfterRequest)
    }


    suspend inline fun <reified T, reified R> makeRequest(metadata: RequestMetadata<T, R>): OpenApiHttpResponse<R> {
        return makeRequest(metadata, typeInfo<T>(), typeInfo<R>())
    }

    suspend fun <T, R> makeRequest(
        metadata: RequestMetadata<T, R>, requestBody: TypeInfo, responseBody: TypeInfo
    ): OpenApiHttpResponse<R> {
        val response = client.request(metadata.url) {
            this.method = metadata.method
            metadata.headers.forEach { key, value -> this.headers.appendAll(key, value) }
            setBody(metadata.body, requestBody)
            beforeRequestHooks.forEach { it(metadata, this) }
        }

        afterRequestHooks.forEach { it(metadata, response) }
        return OpenApiHttpResponse(response, responseBody)
    }
}


@OptIn(io.ktor.util.InternalAPI::class)
private fun <T> HttpRequestBuilder.setBody(body: T, typeInfo: TypeInfo) {
    when (body) {
        null -> {
            this.body = NullBody
            bodyType = typeInfo
        }

        is OutgoingContent -> {
            this.body = body
            bodyType = null
        }

        else -> {
            this.body = body
            bodyType = typeInfo
        }
    }
}
