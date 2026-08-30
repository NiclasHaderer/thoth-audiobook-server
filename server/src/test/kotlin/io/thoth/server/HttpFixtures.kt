package io.thoth.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.thoth.models.LibraryPermissionLevel
import io.thoth.server.database.tables.LibraryUserTable
import io.thoth.server.database.tables.UsersTable
import io.thoth.server.database.tables.toUserRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.assertEquals

const val TEST_PASSWORD = "hunter22"

private val mapper = jacksonObjectMapper()

private fun credentials(username: String) = "{\"username\": \"$username\", \"password\": \"$TEST_PASSWORD\"}"

/**
 * Boots the production plugin and routing stack against the harness database, and registers the first
 * account up front: registration makes the first user an admin, so anyone created inside [block] is a
 * normal user whose access is exactly what the test grants.
 */
fun thothServer(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) =
    testApplication {
        application {
            plugins()
            routing()
        }
        client.register("admin")
        block(client)
    }

suspend fun HttpClient.register(username: String) {
    val response =
        post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(credentials(username))
        }
    assertEquals(HttpStatusCode.Created, response.status, "could not register $username")
}

suspend fun HttpClient.login(username: String): String {
    val response =
        post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(credentials(username))
        }
    assertEquals(HttpStatusCode.OK, response.status, "could not log in $username")
    return mapper.readTree(response.bodyAsText()).get("accessToken").asText()
}

/** Registers a normal user with one library permission and returns their access token. */
suspend fun HttpClient.registerWithAccess(
    username: String,
    libraryId: UUID,
    level: LibraryPermissionLevel,
): String {
    register(username)
    grant(username, libraryId, level)
    return login(username)
}

suspend fun HttpClient.statusOf(
    token: String,
    path: String,
) = get(path) { bearerAuth(token) }.status

suspend fun HttpClient.bodyOf(
    token: String,
    path: String,
): String {
    val response = get(path) { bearerAuth(token) }
    assertEquals(HttpStatusCode.OK, response.status, "$path answered ${response.status}")
    return response.bodyAsText()
}

fun userId(username: String): UUID =
    transaction {
        UsersTable.selectAll().where { UsersTable.username eq username }.single().toUserRow().id
    }

fun grant(
    username: String,
    libraryId: UUID,
    level: LibraryPermissionLevel,
) = transaction {
    val id = userId(username)
    LibraryUserTable.insert {
        it[user] = id
        it[library] = libraryId
        it[permissions] = level
    }
}

fun revoke(
    username: String,
    libraryId: UUID,
) = transaction {
    val id = userId(username)
    LibraryUserTable.deleteWhere { (user eq id) and (library eq libraryId) }
}
