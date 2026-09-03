package io.thoth.server.api

import io.ktor.http.HttpStatusCode
import io.thoth.client.gen.models.ThothLoginUserImpl
import io.thoth.server.TEST_PASSWORD
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.authCookie
import io.thoth.server.bearer
import io.thoth.server.cookie
import io.thoth.server.register
import io.thoth.server.thothServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The refresh window slides, so a client that keeps refreshing never has to log in again. */
class RefreshTokenTest : ThothTest() {
    @Test
    fun `refreshing hands out a new refresh cookie that works on its own`() =
        thothServer {
            register("alice")
            val login = api.loginUser(ThothLoginUserImpl(password = TEST_PASSWORD, username = "alice"))
            val firstRefresh = login.authCookie("refresh")

            val refreshed = api.refreshAccessToken(cookie("refresh", firstRefresh.value))
            assertEquals(HttpStatusCode.OK, refreshed.status)

            val secondRefresh = refreshed.authCookie("refresh")
            assertTrue(
                firstRefresh.maxAge == secondRefresh.maxAge,
                "the new cookie must carry the full window again",
            )

            assertEquals(HttpStatusCode.OK, api.listLibraries(bearer(refreshed.authCookie("access").value)).status)
            assertEquals(HttpStatusCode.OK, api.refreshAccessToken(cookie("refresh", secondRefresh.value)).status)
        }
}
