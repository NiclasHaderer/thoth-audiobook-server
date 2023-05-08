package io.thoth.server.api

data class JWK(val kty: String, val use: String, val kid: String, val n: String, val e: String)

data class JWKs(val keys: List<JWK>)

data class LoginUser(
    var username: String,
    val password: String,
)

data class RegisterUser(var username: String, val password: String)

data class PasswordChange(val currentPassword: String, val newPassword: String)

data class UsernameChange(val username: String)

data class ModifyUser(
    var username: String?,
    val password: String?,
    var admin: Boolean?,
    var edit: Boolean?,
)
