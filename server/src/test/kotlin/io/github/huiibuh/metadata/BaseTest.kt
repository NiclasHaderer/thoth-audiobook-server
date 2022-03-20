package io.github.huiibuh.metadata

import io.github.huiibuh.webServer
import io.ktor.server.testing.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

open class BaseTest {
    protected lateinit var testApp: TestApplicationEngine

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
