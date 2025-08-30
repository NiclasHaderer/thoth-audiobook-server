package io.thoth.server.database.migrations

abstract class Migration {
    abstract fun migrate()
}
