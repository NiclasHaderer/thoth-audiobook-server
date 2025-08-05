package io.thoth.server.plugins.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.thoth.auth.JwtError
import io.thoth.auth.ThothAuthenticationPlugin
import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.models.UserPermissionsModel
import io.thoth.server.common.extensions.findOne
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.access.toExternalUser
import io.thoth.server.database.tables.TUsers
import io.thoth.server.database.tables.User
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject
import java.nio.file.Path

fun Application.configureAuthentication() {
    val thothConfig by inject<ThothConfig>()
    val keyPair = getOrCreateKeyPair(Path.of("${thothConfig.configDirectory}/jwt.pem"))

    install(ThothAuthenticationPlugin.build<UserPermissionsModel>()) {
        production = thothConfig.production
        issuer = "thoth.io"
        domain = thothConfig.domain
        port = thothConfig.port
        protocol = if (thothConfig.TLS) URLProtocol.HTTPS else URLProtocol.HTTP
        jwksPath = "/api/auth/jwks.json"
        keyPairs["thoth"] = keyPair
        activeKeyId = "thoth"

        configureGuard(Guards.Normal) { jwtCredential, setError ->
            jwtToPrincipal(jwtCredential)
                ?: return@configureGuard run {
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
            User.new {
                username = newUser.username
                passwordHash = newUser.passwordHash
                admin = newUser.admin
            }
                .toExternalUser()
        }

        listAllUsers { User.all().map { it.toExternalUser() } }

        deleteUser { User.findById(it.id)?.delete() }

        renameUser { user, newName -> User.findById(user.id)!!.also { it.username = newName }.toExternalUser() }

        updatePassword { user, newPassword ->
            User.findById(user.id)!!.also { it.passwordHash = newPassword }.toExternalUser()
        }

        updateUserPermissions { user, permissions ->
            val dbUser = User.findById(user.id)!!
            dbUser.toExternalUser()
            TODO("Not implemented yet")
        }

        isAdminUser { user: ThothDatabaseUser ->
            transaction {
                User.findById(user.id)?.admin ?: false
            }
        }

        getUserPermissions { user: ThothDatabaseUser ->
            thothPrincipal().permissions
        }
    }
}
