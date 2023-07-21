package io.thoth.server.api

data class JWK(val kty: String, val use: String, val kid: String, val n: String, val e: String)

data class JWKs(val keys: List<JWK>)

data class LoginUser(
    var username: String,
    val password: String,
)

data class RegisterUser(var username: String, val password: String) {
    init {
        // Password requirements
        require(password.isNotBlank()) { "Password must not be blank" }
        require(password.length < 5) { "Password must be at least 5 characters long" }

        // Username validation
        require(username.isNotBlank()) { "Username must not be blank" }
        require(username.length < 5) { "Username must be at least 5 characters long" }
        require(username.matches(Regex("^[a-zA-Z0-9_-]*$"))) { "Username must be alphanumeric, including - and _" }
    }
}

data class PasswordChange(val currentPassword: String, val newPassword: String) {
    init {
        require(newPassword.isNotBlank()) { "New password must not be blank" }
        require(newPassword.length < 5) { "New password must be at least 5 characters long" }
    }
}

data class UsernameChange(val username: String) {
    init {
        // Username validation
        require(username.isNotBlank()) { "Username must not be blank" }
        require(username.length < 5) { "Username must be at least 5 characters long" }
        require(username.matches(Regex("^[a-zA-Z0-9_-]*$"))) { "Username must be alphanumeric, including - and _" }
    }
}

data class ModifyUser(
    val username: String?,
    val password: String?,
    var admin: Boolean?,
    val edit: Boolean?,
) {
    init {
        // Password requirements
        if (password != null) {
            require(password.isNotBlank()) { "Password must not be blank" }
            require(password.length < 5) { "Password must be at least 5 characters long" }
        }

        // Username validation
        if (username != null) {
            require(username.isNotBlank()) { "Username must not be blank" }
            require(username.length < 5) { "Username must be at least 5 characters long" }
            require(username.matches(Regex("^[a-zA-Z0-9_-]*$"))) { "Username must be alphanumeric, including - and _" }
        }
    }
}

data class AccessToken(val accessToken: String)
