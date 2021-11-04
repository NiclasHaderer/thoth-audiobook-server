package io.github.huiibuh.plugins

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.openAPIGen
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Application.configureOpenAPI() {
    install(OpenAPIGen) {
        // basic info
        info {
            version = "0.0.1"
            title = "Audiobook API"
            description = "API for the audiobook server"
        }
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
