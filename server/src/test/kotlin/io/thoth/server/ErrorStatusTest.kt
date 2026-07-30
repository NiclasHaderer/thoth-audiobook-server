package io.thoth.server

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.thoth.metadata.audible.client.AudibleUnavailableException
import io.thoth.openapi.ktor.errors.configureStatusPages
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorStatusTest {
    private fun assertAnswer(
        failure: Throwable,
        expected: HttpStatusCode,
    ) = testApplication {
        application {
            plugins()
            routing { get("/fails") { throw failure } }
        }

        assertEquals(expected, client.get("/fails").status)
    }

    @Test
    fun `an unreachable metadata provider is answered with a bad gateway`() =
        assertAnswer(AudibleUnavailableException("Audible is not answering"), HttpStatusCode.BadGateway)

    @Test
    fun `any other failure is still answered with an internal server error`() =
        assertAnswer(IllegalStateException("something else broke"), HttpStatusCode.InternalServerError)
}
