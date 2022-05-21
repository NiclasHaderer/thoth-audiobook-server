package io.thoth.auth.routes

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

internal class PasswordChange(
    val currentPassword: String,
    val newPassword: String
)
