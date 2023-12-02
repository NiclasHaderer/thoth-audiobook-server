package io.thoth.openapi.ktor.plugins

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.net.URL

internal class WebUiServer(private val config: WebUiConfig) {
    private val notFound = mutableSetOf<String>()
    private val content = mutableMapOf<String, WebUiResource>()

    suspend fun interceptCall(call: ApplicationCall) {
        val pluginConfig = call.application.attributes[OpenAPIConfigurationKey]
        if (!call.request.uri.startsWith(config.docsPath)) {
            return
        }

        if (config.docsPath == call.request.path()) {
            call.respondRedirect("${config.docsPath}/")
            return
        }

        if (isSchemaRequest(call)) {
            respondWithSchema(call, pluginConfig)
        } else if (isStaticRequest(call)) {
            respondWithStatic(call)
        }
    }

    private suspend fun respondWithSchema(call: ApplicationCall, pluginConfig: OpenAPIConfiguration) {
        when (config.schemaType) {
            OpenAPISchemaType.JSON -> {
                call.respondText(ContentType.Application.Json, HttpStatusCode.OK) { pluginConfig.schemaHolder.json() }
            }

            OpenAPISchemaType.YAML -> {
                call.respondText(ContentType.Text.Plain, HttpStatusCode.OK) { pluginConfig.schemaHolder.yaml() }
            }
        }
    }

    private fun isSchemaRequest(call: ApplicationCall): Boolean {
        val callPath = call.request.path()
        val schemaPath = config.schemaPath
        return callPath == schemaPath
    }

    private suspend fun respondWithStatic(call: ApplicationCall) {
        val fileName = getStaticFileName(call)
        call.respond(content[fileName]!!)
    }

    private fun isStaticRequest(call: ApplicationCall): Boolean {
        val webUiVersion = config.webUiVersion
        return when (val fileName = getStaticFileName(call)) {
            in notFound -> false
            "index.html" -> {
                content[fileName] = WebUiResource.index(config.schemaPath)
                return true
            }

            else -> {
                val resource =
                    this::class.java.getResource("/META-INF/resources/webjars/swagger-ui/$webUiVersion/$fileName")
                if (resource == null) {
                    notFound.add(fileName)
                    false
                } else {
                    content[fileName] = WebUiResource(resource)
                    true
                }
            }
        }
    }

    private fun getStaticFileName(call: ApplicationCall): String {
        val callPath = call.request.path()
        val fileName = callPath.removePrefix(config.docsPath).trimEnd('/')

        return fileName
            .ifEmpty {
                return "index.html"
            }
            .trimStart('/')
    }
}

internal class WebUiResource(private val bytes: ByteArray, override val contentType: ContentType) :
    OutgoingContent.ByteArrayContent() {

    constructor(
        url: URL
    ) : this(
        url.readBytes(),
        url.run {
            val extension = file.substring(file.lastIndexOf(".") + 1)
            extensionToContentType[extension] ?: ContentType.Text.Html
        },
    )

    companion object {
        private val extensionToContentType =
            mapOf(
                "html" to ContentType.Text.Html,
                "css" to ContentType.Text.CSS,
                "js" to ContentType.Text.JavaScript,
                "json" to ContentType.Application.Json,
                "png" to ContentType.Image.PNG,
            )

        fun index(openapiURL: String): WebUiResource {
            return WebUiResource(
                // language=HTML
                """
                <!-- HTML for static distribution bundle build -->
                <!DOCTYPE html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8">
                    <title>Swagger UI</title>
                    <link rel="stylesheet" type="text/css" href="./swagger-ui.css" />
                    <link rel="stylesheet" type="text/css" href="index.css" />
                    <link rel="icon" type="image/png" href="./favicon-32x32.png" sizes="32x32" />
                    <link rel="icon" type="image/png" href="./favicon-16x16.png" sizes="16x16" />
                  </head>
                  <body>
                    <div id="swagger-ui"></div>
                    <script src="./swagger-ui-bundle.js" charset="UTF-8"> </script>
                    <script src="./swagger-ui-standalone-preset.js" charset="UTF-8"> </script>
                    <script type="text/javascript">
                    SwaggerUIBundle({
                        url: "$openapiURL",
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [
                          SwaggerUIBundle.presets.apis,
                          SwaggerUIStandalonePreset
                        ],
                        plugins: [
                          SwaggerUIBundle.plugins.DownloadUrl
                        ],
                        layout: "StandaloneLayout"
                      });
                    </script>
                  </body>
                </html>
            """
                    .trimIndent()
                    .toByteArray(),
                ContentType.Text.Html,
            )
        }
    }

    override val contentLength: Long by lazy { bytes.size.toLong() }

    override fun bytes() = bytes
}
