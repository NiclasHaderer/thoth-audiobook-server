package io.thoth.server.plugins.authentication

import com.auth0.jwt.interfaces.Payload
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.pipeline.*
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.tables.User
import java.util.*
import org.jetbrains.exposed.sql.transactions.transaction

class ThothPrincipalImpl(
    val payload: Payload,
    val username: String,
    override val userId: UUID,
    val isEditor: Boolean,
    override val isAdmin: Boolean,
    override val type: ThothJwtTypes,
    override val permissions: Map<String, Any>,
    val accessToLibs: List<UUID>?,
) : ThothPrincipal

fun jwtToPrincipal(credentials: JWTCredential): ThothPrincipalImpl? {
    val username = credentials.payload.getClaim("username").asString() ?: return null
    val edit = credentials.payload.getClaim("edit").asBoolean() ?: return null
    val admin = credentials.payload.getClaim("admin").asBoolean() ?: return null
    val userIdStr = credentials.payload.getClaim("sub").asString() ?: return null
    val userId = UUID.fromString(userIdStr)
    val enumType =
        try {
            val type = credentials.payload.getClaim("type").asString() ?: return null
            ThothJwtTypes.values().first { it.type == type }
        } catch (e: Exception) {
            return null
        }

    val libraries = transaction { User.findById(userId)?.libraries?.map { it.id.value } } ?: return null

    return ThothPrincipalImpl(
        payload = credentials.payload,
        username = username,
        userId = userId,
        isEditor = edit,
        isAdmin = admin,
        type = enumType,
        accessToLibs = libraries.size.takeIf { it > 0 }.let { null },
        permissions = emptyMap(),
    )
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipal(): ThothPrincipalImpl {
    return thothPrincipalOrNull()
        ?: throw ErrorResponse.internalError("Could not get principal. Route has to be guarded with one of the Guards")
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipalOrNull(): ThothPrincipalImpl? = call.principal()

fun PipelineContext<Unit, ApplicationCall>.assertAccessToLibraryId(vararg libraryIds: UUID) {
    val principal = thothPrincipal()
    if (principal.accessToLibs == null) return

    libraryIds.forEach { libraryId ->
        if (!principal.accessToLibs.contains(libraryId)) {
            throw ErrorResponse.forbidden("access", "Library $libraryId")
        }
    }
}
