package io.thoth.openapi

import io.ktor.server.testing.*
import io.thoth.openapi.client.typescript.generateTsClient
import java.io.File
import kotlin.test.Test

class TsGenerationTest {

    @Test
    fun testSomething() = testApplication {
        testRoutes()

        val generatedStuff = mutableMapOf<File, String>()

        generateTsClient(
            dist = "",
            fileWriter = { file, content -> generatedStuff[file] = content },
        )

        println(generatedStuff)
    }
}
