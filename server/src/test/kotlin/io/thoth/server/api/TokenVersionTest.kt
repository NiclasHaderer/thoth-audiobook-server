package io.thoth.server.api

import io.ktor.http.HttpStatusCode
import io.thoth.client.gen.models.ThothChangePasswordImpl
import io.thoth.client.gen.models.ThothLoginUserImpl
import io.thoth.server.TEST_PASSWORD
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.authCookie
import io.thoth.server.bearer
import io.thoth.server.cookie
import io.thoth.server.login
import io.thoth.server.register
import io.thoth.server.thothServer
import io.thoth.server.userId
import kotlin.test.Test
import kotlin.test.assertEquals

/** Changing the password bumps the user's token version, so every previously issued token stops working. */
class TokenVersionTest : ThothTest() {
    @Test
    fun `password change invalidates old access and refresh tokens`() =
        thothServer {
            register("alice")
            val login = api.loginUser(ThothLoginUserImpl(password = TEST_PASSWORD, username = "alice"))
            val oldAccess = login.authCookie("access").value
            val oldRefresh = login.authCookie("refresh").value
            assertEquals(HttpStatusCode.OK, api.listLibraries(bearer(oldAccess)).status)

            val change =
                api.updatePassword(
                    userId("alice"),
                    ThothChangePasswordImpl(currentPassword = TEST_PASSWORD, newPassword = TEST_PASSWORD),
                    bearer(oldAccess),
                )
            assertEquals(HttpStatusCode.NoContent, change.status)

            assertEquals(HttpStatusCode.Unauthorized, api.listLibraries(bearer(oldAccess)).status)
            assertEquals(
                HttpStatusCode.Unauthorized,
                api.refreshAccessToken(cookie("refresh", oldRefresh)).status,
            )

            // The session that changed the password gets fresh cookies and keeps working
            val newAccess = change.authCookie("access").value
            val newRefresh = change.authCookie("refresh").value
            assertEquals(HttpStatusCode.OK, api.listLibraries(bearer(newAccess)).status)
            assertEquals(HttpStatusCode.OK, api.refreshAccessToken(cookie("refresh", newRefresh)).status)
            assertEquals(HttpStatusCode.OK, api.listLibraries(bearer(login("alice"))).status)
        }

    @Test
    fun `deleted user is rejected with the same token`() =
        thothServer {
            register("bob")
            val token = login("bob")
            val admin = login("admin")
            assertEquals(HttpStatusCode.OK, api.listLibraries(bearer(token)).status)

            assertEquals(HttpStatusCode.NoContent, api.deleteUser(userId("bob"), bearer(admin)).status)
            assertEquals(HttpStatusCode.Unauthorized, api.listLibraries(bearer(token)).status)
        }
}
