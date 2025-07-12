package io.thoth.server.plugins.authentication

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.models.UserPermissionsModel
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.di.serialization.Serialization
import io.thoth.server.di.serialization.deserializeValue
import java.util.*

class ThothPrincipalImpl(
    override val userId: UUID,
    override val type: ThothJwtTypes,
    override val permissions: UserPermissionsModel,
) : ThothPrincipal<UUID, UserPermissionsModel>

fun jwtToPrincipal(credentials: JWTCredential, serializer: Serialization): ThothPrincipalImpl? {
    val userIdStr = credentials.payload.getClaim("sub").asString() ?: return null
    val userId = UUID.fromString(userIdStr)
    val enumType =
        try {
            val type = credentials.payload.getClaim("type").asString() ?: return null
            ThothJwtTypes.values().first { it.type == type }
        } catch (_: Exception) {
            return null
        }

    val permissionStr = credentials.payload.getClaim("permissions").asString() ?: return null
    val permission = serializer.deserializeValue<UserPermissionsModel>(permissionStr)
    return ThothPrincipalImpl(userId = userId, type = enumType, permissions = permission)
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipal(): ThothPrincipalImpl {
    return thothPrincipalOrNull()
        ?: throw ErrorResponse.internalError("Could not get principal. Route has to be guarded with one of the Guards")
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipalOrNull(): ThothPrincipalImpl? = call.principal()

fun PipelineContext<Unit, ApplicationCall>.assertLibraryPermissions(vararg libraryIds: UUID) {
    val principal = thothPrincipal()

    val readonlyMethods = listOf(HttpMethod.Head, HttpMethod.Get, HttpMethod.Options)

    libraryIds.forEach { libId ->
        var matches = principal.permissions.libraries.any { allowedLib -> allowedLib.id == libId }
        if (!matches) {
            throw ErrorResponse.forbidden("access", "Library $libId")
        }

        matches =
            (principal.permissions.libraries.any { allowedLib ->
                // If the user is not allowed to edit the library
                !allowedLib.canEdit &&
                    // The used http method is not the readonlyMethods list
                    !readonlyMethods.contains(this.context.request.httpMethod)
            })
        if (!matches) {
            throw ErrorResponse.forbidden("modify", "Library $libId")
        }
    }
}
