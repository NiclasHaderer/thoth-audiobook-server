package io.thoth.openapi

import io.ktor.server.testing.*
import io.thoth.openapi.client.typescript.generateTsClient
import io.thoth.openapi.ktor.OpenApiRouteCollector
import kotlin.test.Test

class TsGenerationTest {

    @Test
    fun testSomething() {
        testApplication {
            testRoutes()
        }

        val generatedStuff = mutableMapOf<String, String>()

        generateTsClient(
            dist = "",
            fileWriter = { file, content -> generatedStuff[file.name] = content },
        )

        println(generatedStuff["models.ts"])

    }
}
