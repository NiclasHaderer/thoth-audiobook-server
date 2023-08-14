package io.thoth.server.plugins.authentication

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.pipeline.*
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.models.UserPermissionsModel
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.util.*

class ThothPrincipalImpl(
    val username: String,
    override val userId: UUID,
    override val type: ThothJwtTypes,
    override val permissions: UserPermissionsModel,
) : ThothPrincipal<UUID, UserPermissionsModel>

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

    val permission = TODO("Get from JWT")

    return ThothPrincipalImpl(username = username, userId = userId, type = enumType, permissions = permission)
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipal(): ThothPrincipalImpl {
    return thothPrincipalOrNull()
        ?: throw ErrorResponse.internalError("Could not get principal. Route has to be guarded with one of the Guards")
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipalOrNull(): ThothPrincipalImpl? = call.principal()

fun PipelineContext<Unit, ApplicationCall>.assertAccessToLibraryId(method: HttpMethod, vararg libraryIds: UUID) {
    val principal = thothPrincipal()

    val readonlyMethods = listOf(HttpMethod.Head, HttpMethod.Get, HttpMethod.Options)

    libraryIds.forEach { libId ->
        val matches =
            principal.permissions.libraries.any { allowedLib ->
                allowedLib.id == libId &&
                    (
                    // If the user is not allowed to edit the library
                    !allowedLib.canEdit &&
                        // The used http method is not the readonlyMethods list
                        !readonlyMethods.contains(method))
            }
        if (!matches) {
            throw ErrorResponse.forbidden("access", "Library $libId")
        }
    }
}
