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
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.security.KeyPair
import java.security.interfaces.RSAPublicKey
import java.util.*


class RegisterUser(
    var username: String,
    val password: String,
    var admin: Boolean,
    var edit: Boolean,
)

class LoginUser(
    var username: String,
    val password: String,
)

class User(
    var id: String,
    var username: String,
    var admin: Boolean,
    var edit: Boolean,
)

fun Route.jwksEndpoint(keyPair: KeyPair) = get {
    val jwk = RSAKey.Builder(keyPair.public as RSAPublicKey)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .build()

    call.respondText(jwk.toString(), ContentType.Application.Json)
}

fun Route.loginEndpoint(config: AuthConfig) = post {
    val user = call.receive<LoginUser>()

    val dbUser = io.thoth.database.tables.User.getByName(user.username) ?: throw ErrorResponse(
        HttpStatusCode.BadRequest,
        "Could not login user"
    )

    val encoder = Argon2PasswordEncoder()
    if (!encoder.matches(user.password, dbUser.passwordHash)) {
        throw ErrorResponse(HttpStatusCode.BadRequest, "Could not login user")
    }

    val jwtPair = generateJwtForUser(config.issuer, user.username, config.keyPair)
    call.respond(jwtPair)
}

fun Route.registerEndpoint(config: AuthConfig) = post {
    val user = call.receive<RegisterUser>()
    val dbUser = io.thoth.database.tables.User.getByName(user.username)
    if (dbUser != null) {
        throw ErrorResponse(HttpStatusCode.BadRequest, "User already exists")
    }

    val encoder = Argon2PasswordEncoder()
    val encodedPassword = encoder.encode(user.password)
    transaction {
        io.thoth.database.tables.User.new {
            username = user.username
            passwordHash = encodedPassword
            admin = user.admin
            edit = user.edit
        }
    }

    val jwtPair = generateJwtForUser(config.issuer, user.username, config.keyPair)
    call.respond(jwtPair)
}


fun Route.userEndpoint() = get {
    val principal = call.principal<ThothPrincipal>()!!
    val u = transaction {
        io.thoth.database.tables.User.getByName(principal.username)
    } ?: throw ErrorResponse(HttpStatusCode.NotFound, "User not found")

    call.respond(u)
}
