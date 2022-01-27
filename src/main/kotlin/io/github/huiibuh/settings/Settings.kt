package io.github.huiibuh.settings

interface Settings {
    val ignoreFile: String
    val production: Boolean
    val audioFileLocation: String
    val analyzerThreads: Int
    val webUiPort: Int
    val audibleSearchHost: String
    val audibleAuthorHost: String
    val database: DatabaseConnection
}
