package io.thoth.openapi.plugin

import io.thoth.openapi.SchemaHolder
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.net.URL


internal class WebUiServer(private val config: WebUiConfig) {
    private val notFound = mutableListOf<String>()
    private val content = mutableMapOf<String, WebUiResource>()


    suspend fun interceptCall(call: ApplicationCall) {
        if (isSchemaRequest(call)) {
            respondWithSchema(call)
        } else if (isStaticRequest(call)) {
            respondWithStatic(call)
        }
    }

    private suspend fun respondWithSchema(call: ApplicationCall) {
        when (config.schemaType) {
            OpenAPISchemaType.JSON -> {
                call.respondText(ContentType.Application.Json, HttpStatusCode.OK) {
                    SchemaHolder.json
                }
            }
            OpenAPISchemaType.YAML -> {
                call.respondText(ContentType.Text.Plain, HttpStatusCode.OK) {
                    SchemaHolder.yaml
                }
            }
        }
    }

    private fun isSchemaRequest(call: ApplicationCall): Boolean {
        val callPath = config.normalizeURL(call.request.path())
        val schemaPath = config.schemaPath
        return callPath == schemaPath
    }

    private suspend fun respondWithStatic(call: ApplicationCall) {
        val fileName = getStaticFileName(call)
        val file = content[fileName]
        if (file != null) {
            call.respond(file)
        }
    }

    private fun isStaticRequest(call: ApplicationCall): Boolean {
        val webUiVersion = config.webUiVersion
        return when (val fileName = getStaticFileName(call)) {
            in notFound -> false
            else -> {
                var newFileName = fileName
                if (fileName == "") {
                    newFileName = "index.html"
                }

                val resource =
                    this::class.java.getResource("/META-INF/resources/webjars/swagger-ui/$webUiVersion/$newFileName")
                if (resource == null) {
                    notFound.add(fileName)
                    false
                } else {
                    content[fileName] = WebUiResource(resource, config.schemaPath)
                    true
                }
            }
        }
    }

    private fun getStaticFileName(call: ApplicationCall): String {
        val callPath = config.normalizeURL(call.request.path())
        return callPath.removePrefix(config.webUiPath).trimEnd('/')
    }
}


private val extensionToContentType = mapOf(
    "html" to ContentType.Text.Html,
    "css" to ContentType.Text.CSS,
    "js" to ContentType.Text.JavaScript,
    "json" to ContentType.Application.Json,
    "png" to ContentType.Image.PNG)

internal class WebUiResource(
    private val url: URL,
    private val schemaURL: String,
) : OutgoingContent.ByteArrayContent() {

    private val bytes by lazy {
        if (contentType == ContentType.Text.Html) {
            url.readText().replace("https://petstore.swagger.io/v2/swagger.json", schemaURL)
                    .toByteArray()
        } else {
            url.readBytes()
        }
    }

    override val contentType: ContentType by lazy {
        val extension = url.file.substring(url.file.lastIndexOf(".") + 1)
        extensionToContentType[extension] ?: ContentType.Text.Html
    }

    override val contentLength: Long? by lazy {
        bytes.size.toLong()
    }

    override fun bytes() = bytes
}
