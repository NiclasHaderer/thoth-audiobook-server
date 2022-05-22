package io.thoth.openapi

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.OpenAPI
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

    internal fun modify(configure: OpenAPI.() -> Unit) {
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
