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
import io.thoth.server.common.extensions.findOne
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.database.tables.LibraryUserEntity
import io.thoth.server.database.tables.LibraryUserTable
import io.thoth.server.database.tables.UserEntity
import io.thoth.server.database.tables.UsersTable
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.ktor.ext.inject

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

// The DB triggers (see 02_ENSURE_ADMIN_EXISTS) enforce "at least one admin"; translate their abort into a 400.
private fun <T> translateLastAdminError(block: () -> T): T =
    try {
        block()
    } catch (e: ExposedSQLException) {
        if (e.message?.contains("only admin", ignoreCase = true) == true) {
            throw ErrorResponse.userError("Cannot remove the only admin user.")
        }
        throw e
    }

fun Application.configureAuthentication() {
    val thothConfig by inject<ThothConfig>()
    val keyPair = getOrCreateKeyPair(thothConfig.jwtKeyFile)

    install(ThothAuthenticationPlugin.build<UserPermissions, UpdateUserPermissions>()) {
        ssl = thothConfig.tls
        issuer = "thoth.io"
        keyPairs["thoth"] = keyPair
        activeKeyId = "thoth"

        configureGuard(Guards.Normal) { jwtCredential, setError ->
            jwtToPrincipal(jwtCredential) ?: return@configureGuard run {
                setError(JwtError("JWT is not valid", HttpStatusCode.Unauthorized))
                null
            }
        }

        configureGuard(Guards.Media, authHeader = bearerFromHeaderOrCookie("access")) { jwtCredential, setError ->
            jwtToPrincipal(jwtCredential) ?: return@configureGuard run {
                setError(JwtError("JWT is not valid", HttpStatusCode.Unauthorized))
                null
            }
        }

        configureGuard(Guards.Admin) { jwtCredential, setError ->
            jwtToPrincipal(jwtCredential)?.let { principal ->
                if (principal.permissions.isAdmin) {
                    principal
                } else {
                    setError(JwtError("User is not an admin", HttpStatusCode.Unauthorized))
                    null
                }
            }
        }

        getUserByUsername { username ->
            transaction { UserEntity.findOne { UsersTable.username eq username }?.toExternalUser() }
        }

        allowNewSignups { thothConfig.allowNewSignups }

        getUserById { transaction { UserEntity.findById(it)?.toExternalUser() } }

        isFirstUser { transaction { UserEntity.count() == 0L } }

        createUser { newUser ->
            transaction {
                rejectDuplicateUsername(newUser.username) {
                    UserEntity
                        .new {
                            username = newUser.username
                            passwordHash = newUser.passwordHash
                            admin = newUser.admin
                        }.also { it.flush() }
                        .toExternalUser()
                }
            }
        }

        listAllUsers { transaction { UserEntity.all().map { it.toExternalUser() } } }

        deleteUser { transaction { translateLastAdminError { UserEntity.findById(it.id)?.delete() } } }

        renameUser { user, newName ->
            transaction {
                rejectDuplicateUsername(newName) {
                    UserEntity.findById(user.id)!!.also { it.username = newName; it.flush() }.toExternalUser()
                }
            }
        }

        updatePassword { user, newPassword ->
            transaction {
                UserEntity.findById(user.id)!!.also { it.passwordHash = newPassword }.toExternalUser()
            }
        }

        updateUserPermissions { currentUser, permissions ->
            transaction {
                val dbUser = UserEntity.findById(currentUser.id)!!

                translateLastAdminError { dbUser.also { it.admin = permissions.isAdmin }.flush() }
                LibraryUserTable.deleteWhere { LibraryUserTable.user eq currentUser.id }
                permissions.libraries.forEach { permission ->
                    val library = LibraryEntity.findById(permission.id)!!
                    LibraryUserEntity.new {
                        this.user = dbUser
                        this.library = library
                        this.permissions = permission.permissions
                    }
                }
                dbUser.toExternalUser()
            }
        }

        isAdminUser { user: ThothDatabaseUser -> transaction { UserEntity.findById(user.id)?.admin ?: false } }

        getUserPermissions { user: ThothDatabaseUser -> resolveUserPermissions(user.id) }
    }
}
