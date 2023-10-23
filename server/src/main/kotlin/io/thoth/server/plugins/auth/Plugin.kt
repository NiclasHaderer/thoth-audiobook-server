package io.thoth.server.plugins.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.thoth.auth.JwtError
import io.thoth.auth.ThothAuthenticationPlugin
import io.thoth.models.UserPermissionsModel
import io.thoth.server.common.extensions.findOne
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.access.wrap
import io.thoth.server.database.tables.TUsers
import io.thoth.server.database.tables.User
import io.thoth.server.database.tables.UserPermissions
import io.thoth.server.di.serialization.Serialization
import io.thoth.server.plugins.authentication.jwtToPrincipal
import java.nio.file.Path
import java.util.*
import org.koin.ktor.ext.inject

fun Application.configureAuthentication() {
    val thothConfig by inject<ThothConfig>()
    val keyPair = getOrCreateKeyPair(Path.of("${thothConfig.configDirectory}/jwt.pem"))

    val serializer by inject<Serialization>()

    install(ThothAuthenticationPlugin.build<UUID, UserPermissionsModel>()) {
        production = thothConfig.production
        issuer = "thoth.io"
        domain = thothConfig.domain
        protocol = if (thothConfig.TLS) URLProtocol.HTTPS else URLProtocol.HTTP
        jwksPath = "/api/auth/jwks.json"
        keyPairs["thoth"] = keyPair
        activeKeyId = "thoth"

        configureGuard(
            Guards.Normal,
        ) { jwtCredential, setError ->
            jwtToPrincipal(jwtCredential)
                ?: return@configureGuard run {
                    setError(
                        JwtError(error = "JWT is not valid", statusCode = HttpStatusCode.Unauthorized),
                    )
                    null
                }
        }

        configureGuard(
            Guards.Admin,
        ) { jwtCredential, setError ->
            jwtToPrincipal(jwtCredential)?.let { principal ->
                if (principal.permissions.isAdmin) {
                    principal
                } else {
                    setError(
                        JwtError(error = "User is not an admin", statusCode = HttpStatusCode.Unauthorized),
                    )
                    null
                }
            }
        }

        getUserByUsername = { username -> User.findOne { TUsers.username eq username }?.wrap() }

        allowNewSignups = { thothConfig.allowNewSignups }

        serializePermissions = { serializer.serializeValue(it) }

        getUserById = { User.findById(it as UUID)?.wrap() }

        isFirstUser = { User.count() == 0L }

        createUser = { newUser ->
            User.new {
                    username = newUser.username
                    passwordHash = newUser.passwordHash
                    permissions = UserPermissions.new { isAdmin = newUser.admin }
                }
                .wrap()
        }

        listAllUsers = { User.all().map { it.wrap() } }

        deleteUser = { User.findById(it.id)?.delete() }

        renameUser = { user, newName -> User.findById(user.id)!!.also { it.username = newName }.wrap() }

        updatePassword = { user, newPassword -> User.findById(user.id)!!.also { it.passwordHash = newPassword }.wrap() }

        updateUserPermissions = { user, permissions ->
            val dbUser = User.findById(user.id)!!
            val dbPermissions = dbUser.permissions
            dbPermissions.isAdmin = permissions.isAdmin
            // TODO library permissions

            dbUser.wrap()
        }
    }
}
