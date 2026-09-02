package io.thoth.server.api

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.cookie
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.setCookie
import io.thoth.server.TEST_PASSWORD
import io.thoth.server.ThothTest
import io.thoth.server.login
import io.thoth.server.register
import io.thoth.server.statusOf
import io.thoth.server.thothServer
import io.thoth.server.userId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Changing the password bumps the user's token version, so every previously issued token stops working. */
class TokenVersionTest : ThothTest() {
    private fun HttpResponse.cookieValue(name: String) = assertNotNull(setCookie().firstOrNull { it.name == name }).value

    @Test
    fun `password change invalidates old access and refresh tokens`() =
        thothServer { client ->
            client.register("alice")
            val login =
                client.post("/api/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"username\": \"alice\", \"password\": \"$TEST_PASSWORD\"}")
                }
            val oldAccess = login.cookieValue("access")
            val oldRefresh = login.cookieValue("refresh")
            assertEquals(HttpStatusCode.OK, client.statusOf(oldAccess, "/api/libraries"))

            val change =
                client.post("/api/auth/user/${userId("alice")}/password") {
                    bearerAuth(oldAccess)
                    contentType(ContentType.Application.Json)
                    setBody("{\"currentPassword\": \"$TEST_PASSWORD\", \"newPassword\": \"$TEST_PASSWORD\"}")
                }
            assertEquals(HttpStatusCode.NoContent, change.status)

            assertEquals(HttpStatusCode.Unauthorized, client.statusOf(oldAccess, "/api/libraries"))
            assertEquals(
                HttpStatusCode.Unauthorized,
                client.post("/api/auth/user/refresh") { cookie("refresh", oldRefresh) }.status,
            )

            // The session that changed the password gets fresh cookies and keeps working
            val newAccess = change.cookieValue("access")
            val newRefresh = change.cookieValue("refresh")
            assertEquals(HttpStatusCode.OK, client.statusOf(newAccess, "/api/libraries"))
            assertEquals(HttpStatusCode.OK, client.post("/api/auth/user/refresh") { cookie("refresh", newRefresh) }.status)
            assertEquals(HttpStatusCode.OK, client.statusOf(client.login("alice"), "/api/libraries"))
        }

    @Test
    fun `deleted user is rejected with the same token`() =
        thothServer { client ->
            client.register("bob")
            val token = client.login("bob")
            val admin = client.login("admin")
            assertEquals(HttpStatusCode.OK, client.statusOf(token, "/api/libraries"))

            val delete = client.delete("/api/auth/user/${userId("bob")}") { bearerAuth(admin) }
            assertEquals(HttpStatusCode.NoContent, delete.status)
            assertEquals(HttpStatusCode.Unauthorized, client.statusOf(token, "/api/libraries"))
        }
}
