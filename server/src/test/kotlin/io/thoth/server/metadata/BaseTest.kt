package io.thoth.server.metadata

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.server.testing.*
import io.thoth.server.webServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

open class BaseTest {
    protected lateinit var testApp: TestApplicationEngine
    val mapper = ObjectMapper()

    @BeforeTest
    fun setup() {
        testApp = TestApplicationEngine(createTestEnvironment())
        testApp.application.webServer()
        testApp.start(true)
    }

    @AfterTest
    fun teardown() {
        testApp.stop(0, 0)
    }

}
