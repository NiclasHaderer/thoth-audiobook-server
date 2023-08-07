package io.thoth.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.thoth.auth.JwtError
import io.thoth.auth.ThothAuthenticationPlugin
import io.thoth.server.common.extensions.findOne
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.access.wrap
import io.thoth.server.database.tables.TUsers
import io.thoth.server.database.tables.User
import io.thoth.server.plugins.authentication.jwtToPrincipal
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.*
import kotlin.io.path.reader
import kotlin.io.path.writer
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.koin.ktor.ext.inject

object Guards {
    const val Admin = "admin"
    const val Editor = "editor"
    const val Normal = "normal"
}

fun Application.configureAuthentication() {
    val thothConfig by inject<ThothConfig>()
    val keyPair = getOrCreateKeyPair(Path.of("${thothConfig.configDirectory}/jwt.pem"))

    install(ThothAuthenticationPlugin) {
        this@install.production = thothConfig.production
        this@install.issuer = "thoth.io"
        this@install.domain = thothConfig.domain
        this@install.protocol = if (thothConfig.TLS) URLProtocol.HTTPS else URLProtocol.HTTP
        this@install.jwksPath = "/api/jwks.json"
        this@install.keyPairs["thoth"] = keyPair
        this@install.activeKeyId = "thoth"

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
                if (principal.isAdmin) {
                    principal
                } else {
                    setError(
                        JwtError(error = "User is not an admin", statusCode = HttpStatusCode.Unauthorized),
                    )
                    null
                }
            }
        }

        configureGuard(
            Guards.Editor,
        ) { jwtCredential, setError ->
            jwtToPrincipal(jwtCredential)?.let { principal ->
                if (principal.isEditor) {
                    principal
                } else {
                    setError(
                        JwtError(error = "User is not an editor", statusCode = HttpStatusCode.Unauthorized),
                    )
                    null
                }
            }
        }

        getUserByUsername = { username -> User.findOne { TUsers.username eq username }?.wrap() }

        getUserById = { User.findById(it as UUID)?.wrap() }

        isFirstUser = { User.count() == 0L }

        createUser = { newUser -> User.new {}.wrap() }

        listAllUsers = { User.all().map { it.wrap() } }

        deleteUser = { User.findById(it.id as UUID)?.delete() }

        renameUser = { user, newName -> User.findById(user.id as UUID)!!.also { it.username = newName }.wrap() }

        updatePassword = { user, newPassword ->
            User.findById(user.id as UUID)!!.also { it.passwordHash = newPassword }.wrap()
        }

        updateUserPermissions = { _, _, _ -> TODO("Not yet implemented") }
    }
}

private fun createKeyPair(jwtKeyFile: Path): KeyPair {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(2048, SecureRandom())
    val kp = kpg.generateKeyPair()

    JcaPEMWriter(jwtKeyFile.writer()).use { pemWriter ->
        pemWriter.writeObject(kp.private)
        pemWriter.writeObject(kp.public)
    }
    return kp
}

private fun getOrCreateKeyPair(path: Path): KeyPair {
    return try {
        path.reader().use { reader ->
            val parsed: PEMKeyPair = PEMParser(reader).readObject() as PEMKeyPair
            JcaPEMKeyConverter().getKeyPair(parsed)
        }
    } catch (e: Exception) {
        createKeyPair(path)
    }
}
