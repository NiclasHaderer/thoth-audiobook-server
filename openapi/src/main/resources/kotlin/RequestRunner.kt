import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.date.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*

typealias OnBeforeRequest<T> = suspend (metadata: RequestMetadata<T>, requestBuilder: HttpRequestBuilder) -> Unit
typealias OnAfterRequest<T, R> = suspend (metadata: RequestMetadata<T>, response: OpenApiHttpResponse<R>) -> Unit


abstract class RequestRunner(
    protected val baseUrl: Url,
) {
    private val beforeRequestHooks: MutableList<OnBeforeRequest<*>> = mutableListOf()
    private val afterRequestHooks: MutableList<OnAfterRequest<*, *>> = mutableListOf()
    private val requestFailedHooks: MutableList<OnAfterRequest<*, *>> = mutableListOf()
    private val client: HttpClient by lazy { createHttpClient() }

    open fun createHttpClient(): HttpClient = HttpClient()

    fun onBeforeRequest(onBeforeRequest: OnBeforeRequest<*>) {
        beforeRequestHooks.add(onBeforeRequest)
    }

    fun onAfterRequest(onAfterRequest: OnAfterRequest<*, *>) {
        afterRequestHooks.add(onAfterRequest)
    }

    fun onRequestFailed(onRequestFailed: OnAfterRequest<*, *>) {
        requestFailedHooks.add(onRequestFailed)
    }

    suspend fun <T, R> makeRequest(
        metadata: RequestMetadata<T>,
        requestBody: TypeInfo,
        responseBody: TypeInfo,
        onBeforeRequest: OnBeforeRequest<T>,
        onAfterRequest: OnAfterRequest<T, R>
    ): OpenApiHttpResponse<R> {
        val finalUrl = URLBuilder(baseUrl).appendEncodedPathSegments(metadata.path).build()
        val response = client.request(finalUrl) {
            this.method = metadata.method
            metadata.headers.forEach { key, value -> this.headers.appendAll(key, value) }
            setBody(metadata.body, requestBody)
            onBeforeRequest(metadata, this)
            beforeRequestHooks.forEach { it(metadata, this) }
        }

        val apiResponse = OpenApiHttpResponse<R>(response, responseBody)
        onAfterRequest(metadata, apiResponse)
        afterRequestHooks.forEach { it(metadata, apiResponse) }
        if (!response.status.isSuccess()) {
            requestFailedHooks.forEach { it(metadata, apiResponse) }
        }
        return apiResponse
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
