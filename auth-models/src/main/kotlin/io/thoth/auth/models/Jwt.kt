package io.thoth.auth.models

interface ThothJwtPair {
    val accessToken: String
    val refreshToken: String
}

enum class ThothJwtTypes(val type: String) {
    Access("access"),
    Refresh("refresh")
}

interface ThothAccessToken {
    val accessToken: String
}
