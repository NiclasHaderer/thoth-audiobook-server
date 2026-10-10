package io.thoth.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.dataconversion.DataConversion
import io.thoth.openapi.ktor.plugins.OpenAPIRouting
import io.thoth.openapi.ktor.plugins.OpenAPIWebUI
import io.thoth.server.config.ThothConfig
import io.thoth.server.plugins.auth.Guards
import org.koin.ktor.ext.inject

fun Application.configureOpenApi() {
    val thothConfig by inject<ThothConfig>()

    install(DataConversion)
    install(OpenAPIRouting) {
        info {
            title = "Thoth"
            version = "0.0.2"
            description = "Audiobook server"
        }
        addServer { url = thothConfig.baseUrl }
        components {
            securitySchemes {
                http(Guards.Admin) {
                    scheme = "bearer"
                    bearerFormat = "JWT"
                }
                http(Guards.Normal) {
                    scheme = "bearer"
                    bearerFormat = "JWT"
                }
                http(Guards.Media) {
                    scheme = "bearer"
                    bearerFormat = "JWT"
                }
            }
        }
    }
    install(OpenAPIWebUI)
}
