package io.thoth.client.gen

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendEncodedPathSegments
import io.ktor.http.content.NullBody
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.InternalAPI

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
    private val client: HttpClient by lazy { createClient() }

    init {
        serialize<Any> { builder, body, typeInfo ->
            builder.contentType(ContentType.Application.Json)
            builder.setBody(body, typeInfo)
        }
        deserialize<Any> { response, typeInfo ->
            response.body(typeInfo)
        }
    }

    open fun HttpClientConfig<*>.configureClient() {}

    open fun createClient() = HttpClient { configureClient() }

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
        onAfterRequest: OnAfterRequest<T, R>,
    ): OpenApiHttpResponse<R> {
        val finalUrl = URLBuilder(baseUrl).appendEncodedPathSegments(metadata.path).build()
        val response =
            client.request(finalUrl) {
                this.method = metadata.method
                val serializer = this@RequestRunner.getClosestSerializer<T>(requestBody.kotlinType!!)
                serializer(this, metadata.body, requestBody)
                metadata.headers.forEach { key, value -> this.headers.appendAll(key, value) }
                onBeforeRequest(metadata, this)
                beforeRequestHooks.forEach { it(metadata, this) }
            }

        val apiResponse =
            OpenApiHttpResponse<R>(
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

@OptIn(InternalAPI::class)
private fun <T> HttpRequestBuilder.setBody(
    body: T,
    typeInfo: TypeInfo,
) {
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
