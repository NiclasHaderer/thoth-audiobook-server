package io.thoth.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.thoth.openapi.client.kotlin.KtErrorHandling
import io.thoth.openapi.client.kotlin.generateKotlinClient
import io.thoth.openapi.client.typescript.generateTsClient
import io.thoth.server.config.ThothConfig
import io.thoth.server.di.setupDependencyInjection
import org.koin.core.context.stopKoin
import kotlin.io.path.createTempDirectory

fun main() {
    val dataDir = createTempDirectory("thoth-client-gen")
    setupDependencyInjection(ThothConfig(dataDir = dataDir))
    try {
        embeddedServer(Netty, port = 0) {
            plugins()
            routing()
            generateTsClient("gen/client/typescript")
            generateKotlinClient(
                apiClientPackageName = "io.thoth.client.gen",
                savePath = "client/src/main/kotlin/io/thoth/client/gen",
                apiClientName = "ThothClient",
                errorHandling = KtErrorHandling.Exception,
            )
        }.apply {
            start(wait = false)
            stop()
        }
    } finally {
        stopKoin()
        dataDir.toFile().deleteRecursively()
    }
}
