package io.thoth.server.plugins

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.openAPIGen
import com.papsign.ktor.openapigen.schema.namer.DefaultSchemaNamer
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureOpenAPI() {
    install(OpenAPIGen) {
        // basic info
        info {
            version = "0.0.1"
            title = "Audiobook API"
            description = "API for the audiobook server"
        }
        replaceModule(DefaultSchemaNamer, DefaultSchemaNamer)
    }
    routing {
        get("/openapi.json") {
            call.respond(application.openAPIGen.api.serialize())
        }
        get("/") {
            call.respondRedirect("/swagger-ui/index.html?url=/openapi.json", true)
        }
    }
}
