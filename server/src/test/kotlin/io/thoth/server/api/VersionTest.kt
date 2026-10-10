package io.thoth.server.api

import io.ktor.http.HttpStatusCode
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.thothServer
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionTest : ThothTest() {
    @Test
    fun `the api version is readable without logging in`() =
        thothServer {
            val response = api.getApiVersion()

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(api.apiVersion, response.body().version)
        }
}
