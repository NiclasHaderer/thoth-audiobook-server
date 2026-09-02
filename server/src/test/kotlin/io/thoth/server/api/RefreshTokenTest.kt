package io.thoth.server.api

import io.ktor.client.request.cookie
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.setCookie
import io.thoth.server.TEST_PASSWORD
import io.thoth.server.ThothTest
import io.thoth.server.register
import io.thoth.server.statusOf
import io.thoth.server.thothServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RefreshTokenTest : ThothTest() {
    @Test
    fun `refreshing hands out a new refresh cookie that works on its own`() =
        thothServer { client ->
            client.register("alice")
            val login =
                client.post("/api/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"username\": \"alice\", \"password\": \"$TEST_PASSWORD\"}")
                }
            val firstRefresh = assertNotNull(login.setCookie().firstOrNull { it.name == "refresh" })

            val refreshed = client.post("/api/auth/user/refresh") { cookie("refresh", firstRefresh.value) }
            assertEquals(HttpStatusCode.OK, refreshed.status)

            val secondRefresh = assertNotNull(refreshed.setCookie().firstOrNull { it.name == "refresh" })
            assertEquals(firstRefresh.maxAge, secondRefresh.maxAge, "the new cookie must carry the full window again")

            val access = assertNotNull(refreshed.setCookie().firstOrNull { it.name == "access" })
            assertEquals(HttpStatusCode.OK, client.statusOf(access.value, "/api/libraries"))
            assertEquals(
                HttpStatusCode.OK,
                client.post("/api/auth/user/refresh") { cookie("refresh", secondRefresh.value) }.status,
            )
        }
}
