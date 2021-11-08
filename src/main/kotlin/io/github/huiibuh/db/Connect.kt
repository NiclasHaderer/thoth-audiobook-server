package io.github.huiibuh.db

fun connectToDatabase() {
    DatabaseFactory.connectAndMigrate()
}
