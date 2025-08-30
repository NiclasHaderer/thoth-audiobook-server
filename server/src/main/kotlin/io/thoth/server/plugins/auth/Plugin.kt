package io.thoth.server.plugins.auth

import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.thoth.auth.JwtError
import io.thoth.auth.ThothAuthenticationPlugin
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
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.ktor.ext.inject
import java.nio.file.Path

fun Application.configureAuthentication() {
    val thothConfig by inject<ThothConfig>()
    val keyPair = getOrCreateKeyPair(Path.of("${thothConfig.configDirectory}/jwt.pem"))

    install(ThothAuthenticationPlugin.build<UserPermissions, UpdateUserPermissions>()) {
        production = thothConfig.production
        issuer = "thoth.io"
        domain = thothConfig.domain
        port = thothConfig.port
        protocol = if (thothConfig.TLS) URLProtocol.HTTPS else URLProtocol.HTTP
        jwksPath = "/api/auth/jwks.json"
        keyPairs["thoth"] = keyPair
        activeKeyId = "thoth"

        configureGuard(Guards.Normal) { jwtCredential, setError ->
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

        getUserByUsername { username -> UserEntity.findOne { UsersTable.username eq username }?.toExternalUser() }

        allowNewSignups { thothConfig.allowNewSignups }

        getUserById { UserEntity.findById(it)?.toExternalUser() }

        isFirstUser { UserEntity.count() == 0L }

        createUser { newUser ->
            UserEntity
                .new {
                    username = newUser.username
                    passwordHash = newUser.passwordHash
                    admin = newUser.admin
                }.toExternalUser()
        }

        listAllUsers { UserEntity.all().map { it.toExternalUser() } }

        deleteUser { UserEntity.findById(it.id)?.delete() }

        renameUser { user, newName -> UserEntity.findById(user.id)!!.also { it.username = newName }.toExternalUser() }

        updatePassword { user, newPassword ->
            transaction {
                UserEntity.findById(user.id)!!.also { it.passwordHash = newPassword }.toExternalUser()
            }
        }

        updateUserPermissions { currentUser, permissions ->
            transaction {
                if (!permissions.isAdmin && UsersTable.selectAll().where { UsersTable.admin eq true }.count() == 1L) {
                    throw ErrorResponse.userError("Cannot remove admin permissions from the only admin user.")
                }

                val dbUser = UserEntity.findById(currentUser.id)!!
                dbUser.admin = permissions.isAdmin
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

        getUserPermissions { user: ThothDatabaseUser -> thothPrincipal().permissions }
    }
}
