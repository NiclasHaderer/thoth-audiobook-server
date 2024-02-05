import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.reflect.*
import kotlin.reflect.KType

typealias OnBeforeRequest<T> = suspend (metadata: RequestMetadata<T>, requestBuilder: HttpRequestBuilder) -> Unit
typealias OnAfterRequest<T, R> = suspend (metadata: RequestMetadata<T>, response: OpenApiHttpResponse<R>) -> Unit

typealias BodySerializer<T> = suspend (builder: HttpRequestBuilder, body: T, typeInfo: TypeInfo) -> Unit
typealias BodyDeserializer<T> = suspend (httpResponse: HttpResponse, typeInfo: TypeInfo) -> T


abstract class RequestRunner(
    protected val baseUrl: Url,
) : SerializationHolder() {
    private val beforeRequestHooks: MutableList<OnBeforeRequest<*>> = mutableListOf()
    private val afterRequestHooks: MutableList<OnAfterRequest<*, *>> = mutableListOf()
    private val requestFailedHooks: MutableList<OnAfterRequest<*, *>> = mutableListOf()
    private val client: HttpClient by lazy { createHttpClient() }

    init {
        serialize<Any> { builder, body, typeInfo ->
            builder.setBody(body, typeInfo)
        }
        deserialize<Any> { response, typeInfo ->
            response.body(typeInfo)
        }
    }

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
            this@RequestRunner.getClosestSerializer<T>(requestBody.kotlinType!!)
                .invoke(this, metadata.body, requestBody)
            onBeforeRequest(metadata, this)
            beforeRequestHooks.forEach { it(metadata, this) }
        }

        val apiResponse = OpenApiHttpResponse<R>(
            response,
            this@RequestRunner.getClosestDeserializer(responseBody.kotlinType!!),
            responseBody,
        )
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
