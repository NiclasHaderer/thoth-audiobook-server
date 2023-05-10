package io.thoth.server.plugins.authentication

import com.auth0.jwt.interfaces.Payload
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.pipeline.*
import io.thoth.generators.openapi.errors.ErrorResponse
import io.thoth.server.database.tables.User
import java.util.*
import org.jetbrains.exposed.sql.transactions.transaction

class ThothPrincipal(
    val payload: Payload,
    val username: String,
    val userId: UUID,
    val edit: Boolean,
    val admin: Boolean,
    val type: JwtType,
    val accessToLibs: List<UUID>?,
) : Principal

fun jwtToPrincipal(credentials: JWTCredential): ThothPrincipal? {
    val username = credentials.payload.getClaim("username").asString() ?: return null
    val edit = credentials.payload.getClaim("edit").asBoolean() ?: return null
    val admin = credentials.payload.getClaim("admin").asBoolean() ?: return null
    val userIdStr = credentials.payload.getClaim("sub").asString() ?: return null
    val userId = UUID.fromString(userIdStr)
    val enumType =
        try {
            val type = credentials.payload.getClaim("type").asString() ?: return null
            JwtType.values().first { it.type == type }
        } catch (e: Exception) {
            return null
        }

    val libraries = transaction { User.findById(userId)?.libraries?.map { it.id.value } } ?: return null

    return ThothPrincipal(
        payload = credentials.payload,
        username = username,
        userId = userId,
        edit = edit,
        admin = admin,
        type = enumType,
        accessToLibs = libraries.size.takeIf { it > 0 }.let { null },
    )
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipal(): ThothPrincipal {
    return thothPrincipalOrNull()
        ?: throw ErrorResponse.internalError("Could not get principal. Route has to be guarded with one of the Guards")
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipalOrNull(): ThothPrincipal? = call.principal()

fun PipelineContext<Unit, ApplicationCall>.assertAccessToLibraryId(vararg libraryIds: UUID) {
    val principal = thothPrincipal()
    if (principal.accessToLibs == null) return

    libraryIds.forEach { libraryId ->
        if (!principal.accessToLibs.contains(libraryId)) {
            throw ErrorResponse.forbidden("access", "Library $libraryId")
        }
    }
}
