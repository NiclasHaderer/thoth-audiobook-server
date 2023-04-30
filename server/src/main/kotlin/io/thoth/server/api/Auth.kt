package io.thoth.server.api

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.server.routing.*
import io.thoth.generators.openapi.delete
import io.thoth.generators.openapi.errors.ErrorResponse
import io.thoth.generators.openapi.get
import io.thoth.generators.openapi.post
import io.thoth.generators.openapi.put
import io.thoth.models.UserModel
import io.thoth.server.authentication.AuthConfigImpl
import io.thoth.server.authentication.JwtPair
import io.thoth.server.authentication.generateJwtForUser
import io.thoth.server.authentication.thothPrincipal
import io.thoth.server.database.access.getById
import io.thoth.server.database.access.getByName
import io.thoth.server.database.access.internalGetByName
import io.thoth.server.database.access.toModel
import io.thoth.server.database.tables.User
import java.security.interfaces.RSAPublicKey
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

fun Routing.authRoutes(config: AuthConfigImpl) {
    post<Api.Auth.Login, LoginUser, JwtPair> { _, user ->
        transaction {
            val userModel =
                User.internalGetByName(user.username) ?: throw ErrorResponse.userError("Could not login user")

            val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
            if (!encoder.matches(user.password, userModel.passwordHash)) {
                throw ErrorResponse.userError("Could not login user")
            }

            generateJwtForUser(config.issuer, userModel, config)
        }
    }

    post<Api.Auth.Register, RegisterUser, UserModel> { _, user ->
        transaction {
            val dbUser = User.getByName(user.username)
            if (dbUser != null) {
                throw ErrorResponse.userError("User with name ${user.username} already exists")
            }

            val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
            val encodedPassword = encoder.encode(user.password)

            val firstUser = User.all().count() == 0L

            User.new {
                    username = user.username
                    passwordHash = encodedPassword
                    admin = firstUser
                    edit = firstUser
                    changePassword = false
                }
                .toModel()
        }
    }

    get<Api.Auth.Jwks, JWKs> {
        val keyPair = config.keyPair
        val jwk = RSAKey.Builder(keyPair.public as RSAPublicKey).keyUse(KeyUse.SIGNATURE).keyID(config.keyId).build()

        JWKs(
            listOf(
                JWK(
                    kty = jwk.keyType.toString(),
                    use = jwk.keyUse.toString(),
                    kid = jwk.keyID,
                    n = jwk.modulus.toString(),
                    e = jwk.publicExponent.toString(),
                ),
            ),
        )
    }

    put<Api.Auth.User.Id, ModifyUser, UserModel> { route, modifyUser ->
        val userID = route.id
        val principal = thothPrincipal()

        transaction {
                val user = User.findById(userID) ?: throw ErrorResponse.notFound("User", userID)

                val editUserIsAdmin = principal.admin
                val editUserIsSelf = principal.userId == user.id.value

                // If the user is not an admin and not editing themselves, they are not allowed to
                // edit the user
                if (!editUserIsAdmin && !editUserIsSelf) {
                    throw ErrorResponse.unauthorized("User")
                }

                user.username = modifyUser.username ?: user.username
                user.edit = modifyUser.edit ?: user.edit
                user.changePassword = modifyUser.changePassword ?: user.changePassword

                if (modifyUser.password != null) {
                    val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
                    val encodedPassword = encoder.encode(modifyUser.password)
                    user.passwordHash = encodedPassword
                }

                // Only admins can change admin status and enabled status
                if (editUserIsAdmin) {
                    user.admin = modifyUser.admin ?: user.admin
                    user.enabled = modifyUser.enabled ?: user.enabled
                }

                user
            }
            .toModel()
    }

    get<Api.Auth.User, UserModel> {
        val principal = thothPrincipal()
        User.getById(principal.userId) ?: throw ErrorResponse.notFound("User", principal.userId)
    }

    delete<Api.Auth.User, Unit, Unit> { _, _,
        ->
        val principal = thothPrincipal()
        User.findById(principal.userId)?.delete() ?: throw ErrorResponse.notFound("User", principal.userId)
    }

    post<Api.Auth.User.Username, UsernameChange, UserModel> { _, usernameChange ->
        val principal = thothPrincipal()

        transaction {
            val user = User.findById(principal.userId) ?: throw ErrorResponse.notFound("User", principal.userId)
            val existingUser = User.getByName(usernameChange.username)
            if (existingUser != null) {
                throw ErrorResponse.userError("User with name ${usernameChange.username} already exists")
            }
            user.username = usernameChange.username
            user.toModel()
        }
    }

    post<Api.Auth.User.Password, PasswordChange, Unit> { _, passwordChange ->
        if (passwordChange.currentPassword == passwordChange.newPassword) {
            throw ErrorResponse.userError("New password is the same as the current one")
        }

        val principal = thothPrincipal()
        transaction {
            val user = User.findById(principal.userId) ?: throw ErrorResponse.notFound("User", principal.userId)

            val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
            if (!encoder.matches(passwordChange.currentPassword, user.passwordHash)) {
                throw ErrorResponse.userError("Old password is wrong.")
            }
            user.passwordHash = encoder.encode(passwordChange.newPassword)
        }
    }
}
