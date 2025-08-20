package io.thoth.server.plugins.auth

import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.thoth.auth.JwtError
import io.thoth.auth.ThothAuthenticationPlugin
import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.models.UpdatePermissionsModel
import io.thoth.models.UserPermissionsModel
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.extensions.findOne
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.access.toExternalUser
import io.thoth.server.database.tables.Library
import io.thoth.server.database.tables.LibraryUserMappingEntity
import io.thoth.server.database.tables.TLibraryUserMapping
import io.thoth.server.database.tables.TUsers
import io.thoth.server.database.tables.User
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.ktor.ext.inject
import java.nio.file.Path

fun Application.configureAuthentication() {
    val thothConfig by inject<ThothConfig>()
    val keyPair = getOrCreateKeyPair(Path.of("${thothConfig.configDirectory}/jwt.pem"))

    install(ThothAuthenticationPlugin.build<UserPermissionsModel, UpdatePermissionsModel>()) {
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

        getUserByUsername { username -> User.findOne { TUsers.username eq username }?.toExternalUser() }

        allowNewSignups { thothConfig.allowNewSignups }

        getUserById { User.findById(it)?.toExternalUser() }

        isFirstUser { User.count() == 0L }

        createUser { newUser ->
            User
                .new {
                    username = newUser.username
                    passwordHash = newUser.passwordHash
                    admin = newUser.admin
                }.toExternalUser()
        }

        listAllUsers { User.all().map { it.toExternalUser() } }

        deleteUser { User.findById(it.id)?.delete() }

        renameUser { user, newName -> User.findById(user.id)!!.also { it.username = newName }.toExternalUser() }

        updatePassword { user, newPassword ->
            transaction {
                User.findById(user.id)!!.also { it.passwordHash = newPassword }.toExternalUser()
            }
        }

        updateUserPermissions { currentUser, permissions ->
            transaction {
                if (!permissions.isAdmin && TUsers.selectAll().where { TUsers.admin eq true }.count() == 1L) {
                    throw ErrorResponse.userError("Cannot remove admin permissions from the only admin user.")
                }

                val dbUser = User.findById(currentUser.id)!!
                dbUser.admin = permissions.isAdmin
                TLibraryUserMapping.deleteWhere { TLibraryUserMapping.user eq currentUser.id }
                permissions.libraries.forEach { permission ->
                    val library = Library.findById(permission.id)!!
                    LibraryUserMappingEntity.new {
                        this.user = dbUser
                        this.library = library
                        this.permissions = permission.permissions
                    }
                }
                dbUser.toExternalUser()
            }
        }

        isAdminUser { user: ThothDatabaseUser -> transaction { User.findById(user.id)?.admin ?: false } }

        getUserPermissions { user: ThothDatabaseUser -> thothPrincipal().permissions }
    }
}
