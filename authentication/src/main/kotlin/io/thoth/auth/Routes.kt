package io.thoth.auth

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.thoth.common.exceptions.ErrorResponse
import io.thoth.database.tables.User
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.security.interfaces.RSAPublicKey
import java.util.*


fun Route.jwksEndpoint(config: AuthConfig) = get {
    val keyPair = config.keyPair
    val jwk = RSAKey.Builder(keyPair.public as RSAPublicKey)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(config.keyId)
        .build()


    call.respond(mapOf("keys" to arrayOf(jwk.toJSONObject())))
}

class LoginUser(
    var username: String,
    val password: String,
)

fun Route.loginEndpoint(config: AuthConfig) = post {
    val user = call.receive<LoginUser>()

    val userModel = User.getByName(user.username) ?: throw ErrorResponse(
        HttpStatusCode.BadRequest,
        "Could not login user"
    )

    val encoder = Argon2PasswordEncoder()
    if (!encoder.matches(user.password, userModel.passwordHash)) {
        throw ErrorResponse(HttpStatusCode.BadRequest, "Could not login user")
    }

    val jwtPair = generateJwtForUser(config.issuer, userModel, config)
    call.respond(jwtPair)
}

class RegisterUser(
    var username: String,
    val password: String,
    var admin: Boolean,
    var edit: Boolean,
)

fun Route.registerEndpoint(config: AuthConfig) = post {
    val user = call.receive<RegisterUser>()
    val dbUser = User.getByName(user.username)
    if (dbUser != null) {
        throw ErrorResponse(HttpStatusCode.BadRequest, "User already exists")
    }

    val encoder = Argon2PasswordEncoder()
    val encodedPassword = encoder.encode(user.password)
    val userModel = transaction {
        User.new {
            username = user.username
            passwordHash = encodedPassword
            admin = user.admin
            edit = user.edit
        }.toModel()
    }

    val jwtPair = generateJwtForUser(config.issuer, userModel, config)
    call.respond(jwtPair)
}

fun Route.modifyUser() = patch {
    val postModel = call.receive<RegisterUser>()
    val userID = try {
        UUID.fromString(call.request.queryParameters["userID"])
    } catch (e: Exception) {
        throw ErrorResponse(HttpStatusCode.BadRequest, "Could not decode user id")
    }

    val userModel = transaction {
        val user = User.findById(userID) ?: throw ErrorResponse(
            HttpStatusCode.BadRequest,
            "Could not find user with id $userID"
        )
        user.edit = postModel.edit
        user.admin = postModel.admin
        user.username = postModel.username
        user
    }.toModel().toPublicModel()

    call.respond(userModel)
}

class PasswordChange(
    val currentPassword: String,
    val newPassword: String
)

fun Route.changePassword() = post {
    val passwordChange = call.receive<PasswordChange>()

    if (passwordChange.currentPassword == passwordChange.newPassword) {
        throw ErrorResponse(HttpStatusCode.BadRequest, "New password is the same as the current one")
    }
    val principal = call.principal<ThothPrincipal>()!!

    transaction {
        val user = User.findById(principal.userId) ?: throw ErrorResponse(
            HttpStatusCode.BadRequest,
            "Could not find user with id ${principal.userId}"
        )

        val encoder = Argon2PasswordEncoder()
        if (!encoder.matches(passwordChange.currentPassword, user.passwordHash)) {
            throw ErrorResponse(HttpStatusCode.BadRequest, "Could change password. Old password is wrong.")
        }
        user.passwordHash = encoder.encode(passwordChange.newPassword)
    }

    call.respondText("", ContentType.Application.Json)
}


fun Route.userEndpoint() = get {
    val principal = call.principal<ThothPrincipal>()!!
    val userModel = transaction {
        User.getByName(principal.username)?.toPublicModel()
    } ?: throw ErrorResponse(HttpStatusCode.NotFound, "User not found")

    call.respond(userModel)
}
