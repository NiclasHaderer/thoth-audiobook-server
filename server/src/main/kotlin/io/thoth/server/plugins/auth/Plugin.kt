package io.thoth.server.plugins.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.thoth.auth.JwtError
import io.thoth.auth.ThothAuthenticationPlugin
import io.thoth.auth.bearerFromHeaderOrCookie
import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.models.UpdateUserPermissions
import io.thoth.models.UserPermissions
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.tables.LibraryTable
import io.thoth.server.database.tables.LibraryUserTable
import io.thoth.server.database.tables.UserRow
import io.thoth.server.database.tables.UserTable
import io.thoth.server.database.tables.insert
import io.thoth.server.database.tables.toUserRow
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.ktor.ext.inject
import java.util.UUID

private fun <T> rejectDuplicateUsername(
    username: String,
    block: () -> T,
): T =
    try {
        block()
    } catch (e: ExposedSQLException) {
        if (e.message?.contains("username", ignoreCase = true) == true) {
            throw ErrorResponse.userError("User with name $username already exists")
        }
        throw e
    }

context(_: Transaction)
internal fun userRow(id: UUID): UserRow? =
    UserTable
        .selectAll()
        .where { UserTable.id eq id }
        .firstOrNull()
        ?.toUserRow()

fun Application.configureAuthentication() {
    val thothConfig by inject<ThothConfig>()
    val keyPair = getOrCreateKeyPair(thothConfig.jwtKeyFile)

    install(ThothAuthenticationPlugin.build<UserPermissions, UpdateUserPermissions>()) {
        ssl = thothConfig.tls
        issuer = "thoth.io"
        keyPairs["thoth"] = keyPair
        activeKeyId = "thoth"

        configureGuard(Guards.Normal) { user, _ -> ThothPrincipalImpl(user.id) }

        configureGuard(
            Guards.Media,
            authHeader = bearerFromHeaderOrCookie("access"),
        ) { user, _ -> ThothPrincipalImpl(user.id) }

        configureGuard(Guards.Admin) { user, setError ->
            ThothPrincipalImpl(user.id).takeIf { it.permissions.isAdmin } ?: run {
                setError(JwtError("User is not an admin", HttpStatusCode.Unauthorized))
                null
            }
        }

        getUserByUsername { username ->
            transaction {
                UserTable
                    .selectAll()
                    .where { UserTable.username eq username }
                    .firstOrNull()
                    ?.toUserRow()
                    ?.toExternalUser()
            }
        }

        allowNewSignups { thothConfig.allowNewSignups }

        getUserById { transaction { userRow(it)?.toExternalUser() } }

        isFirstUser { transaction { UserTable.selectAll().count() == 0L } }

        createUser { newUser ->
            transaction {
                rejectDuplicateUsername(newUser.username) {
                    val row =
                        UserRow(
                            id = UUID.randomUUID(),
                            username = newUser.username,
                            passwordHash = newUser.passwordHash,
                            admin = newUser.admin,
                            tokenVersion = 0,
                        )
                    UserTable.insert(row)
                    row.toExternalUser()
                }
            }
        }

        listAllUsers { transaction { UserTable.selectAll().map { it.toUserRow().toExternalUser() } } }

        deleteUser {
            transaction {
                val dbUser = userRow(it.id) ?: return@transaction
                if (dbUser.admin) requireAnotherAdminExists(it.id)
                UserTable.deleteWhere { UserTable.id eq dbUser.id }
            }
        }

        renameUser { user, newName ->
            transaction {
                rejectDuplicateUsername(newName) {
                    UserTable.update({ UserTable.id eq user.id }) { it[username] = newName }
                    userRow(user.id)!!.toExternalUser()
                }
            }
        }

        updatePassword { user, newPassword ->
            transaction {
                UserTable.update({ UserTable.id eq user.id }) {
                    it[passwordHash] = newPassword
                    it[tokenVersion] = tokenVersion + 1
                }
                userRow(user.id)!!.toExternalUser()
            }
        }

        updateUserPermissions { currentUser, permissions ->
            transaction {
                val dbUser = userRow(currentUser.id)!!

                if (dbUser.admin && !permissions.isAdmin) requireAnotherAdminExists(currentUser.id)
                UserTable.update({ UserTable.id eq dbUser.id }) { it[admin] = permissions.isAdmin }
                LibraryUserTable.deleteWhere { LibraryUserTable.user eq currentUser.id }
                permissions.libraries.forEach { permission ->
                    val libraryId =
                        LibraryTable
                            .select(LibraryTable.id)
                            .where { LibraryTable.id eq permission.id }
                            .single()[LibraryTable.id]
                    // TODO We are not super consistent with how we handle inserts. Sometimes we
                    //  require the [table]Row, someitmes we do this instead. But this is a probelm for
                    //  tomorrows Niclas...
                    LibraryUserTable.insert {
                        it[user] = dbUser.id
                        it[library] = libraryId
                        it[LibraryUserTable.permissions] = permission.permissions
                    }
                }
                userRow(dbUser.id)!!.toExternalUser()
            }
        }

        isAdminUser { user: ThothDatabaseUser -> transaction { userRow(user.id)?.admin ?: false } }

        getUserPermissions { user: ThothDatabaseUser -> resolveUserPermissions(user.id) }
    }
}
