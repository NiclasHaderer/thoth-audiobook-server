package io.github.huiibuh.plugins

import io.github.huiibuh.db.DatabaseFactory
import io.ktor.application.*

fun Application.connectToDatabase() {
    DatabaseFactory.connectAndMigrate()
}
