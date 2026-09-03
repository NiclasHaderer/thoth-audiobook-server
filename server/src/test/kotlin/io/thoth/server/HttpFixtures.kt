package io.thoth.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.thoth.client.gen.models.LibraryPermissionLevel
import io.thoth.client.gen.models.ThothLoginUserImpl
import io.thoth.client.gen.models.ThothModifyPermissionsImpl
import io.thoth.client.gen.models.ThothRegisterUserImpl
import io.thoth.client.gen.models.UpdateLibraryPermissionsImpl
import io.thoth.client.gen.models.UpdateUserPermissionsImpl
import java.util.UUID
import kotlin.test.assertEquals

const val TEST_PASSWORD = "hunter22"

/**
 * Boots the production plugin and routing stack against the harness database, and registers the first
 * account up front: registration makes the first user an admin, so anyone created inside [block] is a
 * normal user whose access is exactly what the test grants.
 */
fun thothServer(block: suspend ApplicationTestBuilder.() -> Unit) =
    testApplication {
        application {
            plugins()
            routing()
        }
        register("admin")
        block()
    }

suspend fun ApplicationTestBuilder.register(username: String) {
    val response = api.registerUser(ThothRegisterUserImpl(password = TEST_PASSWORD, username = username))
    assertEquals(HttpStatusCode.Created, response.status, "could not register $username")
}

suspend fun ApplicationTestBuilder.login(username: String): String {
    val response = api.loginUser(ThothLoginUserImpl(password = TEST_PASSWORD, username = username))
    assertEquals(HttpStatusCode.OK, response.status, "could not log in $username")
    return response.body().accessToken
}

suspend fun ApplicationTestBuilder.registerWithAccess(
    username: String,
    libraryId: UUID,
    level: LibraryPermissionLevel,
): String {
    register(username)
    grant(username, libraryId, level)
    return login(username)
}

// The endpoint replaces the whole permission set, so grant and revoke read the current one first
suspend fun ApplicationTestBuilder.setLibraries(
    username: String,
    libraries: List<UpdateLibraryPermissionsImpl>,
) {
    val admin = bearer(login("admin"))
    val response =
        api.updatePermissions(
            userId(username),
            ThothModifyPermissionsImpl(UpdateUserPermissionsImpl(isAdmin = false, libraries = libraries)),
            admin,
        )
    assertEquals(HttpStatusCode.OK, response.status, "could not set permissions for $username")
}

suspend fun ApplicationTestBuilder.grant(
    username: String,
    libraryId: UUID,
    level: LibraryPermissionLevel,
) = setLibraries(username, currentLibraries(username) + UpdateLibraryPermissionsImpl(libraryId, level))

suspend fun ApplicationTestBuilder.revoke(
    username: String,
    libraryId: UUID,
) = setLibraries(username, currentLibraries(username).filterNot { it.id == libraryId })

private suspend fun ApplicationTestBuilder.currentLibraries(username: String) =
    user(username).permissions.libraries.map { UpdateLibraryPermissionsImpl(it.id, it.permissions) }

suspend fun ApplicationTestBuilder.userId(username: String): UUID = user(username).id

private suspend fun ApplicationTestBuilder.user(username: String) =
    api.listUsers(bearer(login("admin"))).body().single { it.username == username }
