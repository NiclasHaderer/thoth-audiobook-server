package io.github.huiibuh

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import io.ktor.server.testing.*

open class BaseTest {
    protected lateinit var testApp: TestApplicationEngine

    @BeforeTest
    fun setup() {
        testApp = TestApplicationEngine(createTestEnvironment())
        testApp.application.module()
        testApp.start(true)
    }

    @AfterTest
    fun teardown() {
        testApp.stop(0, 0)
    }

}
