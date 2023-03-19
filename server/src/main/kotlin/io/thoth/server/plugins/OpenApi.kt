package io.thoth.server.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.dataconversion.*
import io.thoth.auth.Guards
import io.thoth.openapi.plugin.OpenAPIRouting
import io.thoth.openapi.plugin.OpenAPIWebUI

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
