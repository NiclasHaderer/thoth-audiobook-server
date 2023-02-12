package io.thoth.auth.routes

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable
import java.util.*

internal class LoginUser(
    var username: String,
    val password: String,
)

internal class RegisterUser(
    var username: String,
    val password: String,
    var admin: Boolean,
    var edit: Boolean,
)

internal class EditUser(
    var username: String?,
    val password: String?,
    var admin: Boolean?,
    var edit: Boolean?,
    val enabled: Boolean?,
    val changePassword: Boolean?
)

internal class PasswordChange(
    val currentPassword: String,
    val newPassword: String
)

internal class UsernameChange(
    val username: String
)

@Serializable
@Resource("{id}")
internal class IdRoute(
    val id: UUID_S
)
