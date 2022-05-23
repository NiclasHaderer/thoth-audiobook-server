package io.thoth.openapi

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import java.util.concurrent.atomic.AtomicBoolean

object SchemaHolder {
    private var api: OpenAPI? = null
    private val _finalized = AtomicBoolean(false)

    val finalized
        get() = _finalized.get()

    val json: String by lazy {
        if (!finalized) throw Error("Schema has not finalized yet")
        Json.mapper().writeValueAsString(this.api)
    }

    val yaml: String by lazy {
        if (!finalized) throw Error("Schema has not finalized yet")
        Yaml.mapper().writeValueAsString(this.api)
    }

    internal fun lock() {
        this._finalized.set(true)
    }

    internal fun set(api: OpenAPI) {
        if (this.api != null) {
            throw Error("Schema has already been set")
        }
        this.api = api
    }

    fun copy(): OpenAPI {
        val mapper = ObjectMapper()
        val string = mapper.writeValueAsString(api ?: OpenAPI())
        return mapper.readValue(string, OpenAPI::class.java)
    }

    fun addRouteToApi(
        url: String,
        method: HttpMethod,
        requestBody: Any,
        queryParams: Any,
        pathParams: Any,
        responseBody: Any,
        responseStatus: HttpStatusCode
    ) {
        modify {
            if (paths == null) this.paths = Paths()

            val pathItem = paths.getOrPut(url) { PathItem() }

            val operation = Operation()
            when (method) {
                HttpMethod.Get -> pathItem.get = operation
                HttpMethod.Head -> pathItem.head = operation
                HttpMethod.Post -> pathItem.post = operation
                HttpMethod.Put -> pathItem.put = operation
                HttpMethod.Delete -> pathItem.delete = operation
                HttpMethod.Options -> pathItem.options = operation
                HttpMethod.Patch -> pathItem.patch = operation
            }

            operation.responses = ApiResponses().addApiResponse(
                responseStatus.value.toString(),
                ApiResponse()
                    .description("TODO")
                    .content(
                        Content().addMediaType(
                            "application/json",
                            MediaType().schema(Schema<Unit>().type("object"))
                        )
                    )
            )
        }
    }

    fun modify(configure: OpenAPI.() -> Unit) {
        if (this.api == null) {
            throw Error("Schema has not been set")
        }
        if (finalized) {
            throw Error("Application startup has completed. You cannot modify the API object any more")
        }

        synchronized(this.api!!) {
            this.api!!.configure()
        }
    }
}
