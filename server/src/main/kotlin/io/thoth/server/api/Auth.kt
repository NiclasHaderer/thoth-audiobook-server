package io.thoth.server.api

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import io.thoth.models.UserModel
import io.thoth.openapi.ktor.delete
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.post
import io.thoth.openapi.ktor.put
import io.thoth.server.common.extensions.asUUID
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.access.*
import io.thoth.server.database.tables.User
import io.thoth.server.plugins.authentication.*
import java.security.interfaces.RSAPublicKey
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

fun Routing.authRoutes() {
    val authConfig by inject<AuthConfig>()
    val config by inject<ThothConfig>()

    post<Api.Auth.Login, LoginUser, AccessToken> { _, user ->
        transaction {
            val userModel =
                User.internalGetByName(user.username)
                    ?: throw ErrorResponse.userError(
                        if (config.production) "Could not login user" else "Could not find user with username"
                    )

            val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
            if (!encoder.matches(user.password, userModel.passwordHash)) {
                throw ErrorResponse.userError(if (config.production) "Could not login user" else "Wrong password")
            }

            val keyPair = generateJwtForUser(userModel, authConfig)

            call.response.cookies.append(
                Cookie(
                    name = "refresh",
                    value = keyPair.refresh,
                    httpOnly = true,
                    secure = config.TLS,
                    extensions = mapOf("SameSite" to "Strict", "HostOnly" to "true"),
                    maxAge = (REFRESH_TOKEN_EXPIRY_MS / 1000).toInt(),
                ),
            )

            AccessToken(keyPair.access)
        }
    }

    post<Api.Auth.Logout, Unit, Unit> { _, _ ->
        call.response.cookies.append(
            Cookie(
                name = "refresh",
                value = "",
                httpOnly = true,
                secure = config.TLS,
                extensions = mapOf("SameSite" to "Strict", "HostOnly" to "true"),
                expires = GMTDate(0),
            ),
        )
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
                }
                .toModel()
        }
    }

    get<Api.Auth.Jwks, JWKs> {
        val keyPair = authConfig.keyPair
        val jwk =
            RSAKey.Builder(keyPair.public as RSAPublicKey).keyUse(KeyUse.SIGNATURE).keyID(authConfig.keyId).build()

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

    put<Api.Auth.User.Edit, ModifyUser, UserModel> { route, modifyUser ->
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

                if (modifyUser.password != null) {
                    val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
                    val encodedPassword = encoder.encode(modifyUser.password)
                    user.passwordHash = encodedPassword
                }

                // Only admins can change admin status
                if (editUserIsAdmin) {
                    user.admin = modifyUser.admin ?: user.admin
                }

                user
            }
            .toModel()
    }

    get<Api.Auth.User, UserModel> {
        val principal = thothPrincipal()
        transaction { User.getById(principal.userId) ?: throw ErrorResponse.notFound("User", principal.userId) }
    }

    get<Api.Auth.User.All, List<UserModel>> { transaction { User.all().map { it.toModel() } } }

    delete<Api.Auth.User, Unit, Unit> { _, _,
        ->
        val principal = thothPrincipal()
        transaction {
            User.findById(principal.userId)?.delete()
                ?: throw ErrorResponse.notFound(
                    "User",
                    principal.userId,
                )
        }
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

    post<Api.Auth.User.Refresh, Unit, AccessToken> { _, _ ->
        val refreshToken = call.request.cookies["refresh"] ?: throw ErrorResponse.unauthorized("No refresh token")

        val decodedJwt = validateJwt(authConfig, refreshToken, JwtType.Refresh)
        val userId = decodedJwt.getClaim("sub").asString().asUUID()
        val user =
            transaction { User.findById(userId)?.toInternalModel() } ?: throw ErrorResponse.notFound("User", userId)

        AccessToken(
            generateAccessTokenForUser(user, authConfig),
        )
    }
}
