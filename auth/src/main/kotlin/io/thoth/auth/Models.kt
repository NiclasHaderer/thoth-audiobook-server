package io.thoth.auth

interface ThothLoginUser {
    val username: String
    val password: String
}

interface ThothRegisterUser {
    val username: String
    val password: String
}

interface ThothDatabaseUser {
    val id: Any
    val username: String
    val password: String
    val admin: Boolean
}

interface ThothJwtPair {
    val accessToken: String
    val refreshToken: String
}

class ThothJwtPairImpl(
    override val accessToken: String,
    override val refreshToken: String,
) : ThothJwtPair

enum class ThothJwtTypes(val type: String) {
    Access("access"),
    Refresh("refresh")
}

interface ThothAccessToken {
    val accessToken: String
}

class ThothAccessTokenImpl(
    override val accessToken: String,
) : ThothAccessToken
