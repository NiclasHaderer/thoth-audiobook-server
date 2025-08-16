package io.thoth.server.plugins.auth

import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.principal
import io.ktor.server.request.httpMethod
import io.ktor.util.pipeline.PipelineContext
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.models.LibraryPermissions
import io.thoth.models.LibraryPermissionsModel
import io.thoth.models.UserPermissionsModel
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.tables.TLibraries
import io.thoth.server.database.tables.TLibraryUserMapping
import io.thoth.server.database.tables.User
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ThothPrincipalImpl(
    override val userId: UUID,
    override val type: ThothJwtTypes,
) : ThothPrincipal {
    val permissions: UserPermissionsModel
        get() =
            transaction {
                val user = User.findById(userId) ?: throw ErrorResponse.notFound("User", userId)
                val permissions: List<LibraryPermissionsModel>
                if (user.admin) {
                    permissions =
                        TLibraries.selectAll().map {
                            LibraryPermissionsModel(
                                id = it[TLibraries.id].value,
                                permissions = LibraryPermissions.READ_WRITE,
                                name = it[TLibraries.name],
                            )
                        }
                } else {
                    permissions =
                        (TLibraryUserMapping innerJoin TLibraries)
                            .select { TLibraryUserMapping.user eq userId }
                            .map {
                                LibraryPermissionsModel(
                                    id = it[TLibraryUserMapping.library].value,
                                    permissions = it[TLibraryUserMapping.permissions],
                                    name = it[TLibraries.name],
                                )
                            }
                }
                UserPermissionsModel(isAdmin = user.admin, libraries = permissions)
            }
}

fun jwtToPrincipal(credentials: JWTCredential): ThothPrincipalImpl? {
    val userIdStr = credentials.payload.getClaim("sub").asString() ?: return null
    val userId = UUID.fromString(userIdStr)
    val enumType =
        try {
            val type = credentials.payload.getClaim("type").asString() ?: return null
            ThothJwtTypes.entries.first { it.type == type }
        } catch (_: Exception) {
            return null
        }

    return ThothPrincipalImpl(userId = userId, type = enumType)
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipal(): ThothPrincipalImpl =
    thothPrincipalOrNull()
        ?: throw ErrorResponse.internalError("Could not get principal. Route has to be guarded with one of the Guards")

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
            (
                principal.permissions.libraries.any { allowedLib ->
                    // If the user is not allowed to edit the library
                    allowedLib.permissions == LibraryPermissions.READ_WRITE &&
                        // The used http method is not the readonlyMethods list
                        !readonlyMethods.contains(this.context.request.httpMethod)
                }
            )
        if (!matches) {
            throw ErrorResponse.forbidden("modify", "Library $libId")
        }
    }
}
