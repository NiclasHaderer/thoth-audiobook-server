package io.thoth.server.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.dataconversion.*
import io.thoth.generators.openapi.plugins.OpenAPIRouting
import io.thoth.generators.openapi.plugins.OpenAPIWebUI
import io.thoth.server.authentication.Guards

fun Application.configureOpenApi() {
    install(DataConversion)
    install(OpenAPIRouting) {
        info {
            title = "Thoth"
            version = "0.0.1"
            description = "Audiobook server"
        }
        components {
            securitySchemes {
                http(Guards.Admin) {
                    scheme = "bearer"
                    bearerFormat = "JWT"
                }
                http(Guards.Edit) {
                    scheme = "bearer"
                    bearerFormat = "JWT"
                }
                http(Guards.Normal) {
                    scheme = "bearer"
                    bearerFormat = "JWT"
                }
            }
        }
    }
    install(OpenAPIWebUI)
}
