package io.thoth.server.config

import com.cronutils.model.Cron
import io.thoth.metadata.audible.models.AudibleRegions

interface ThothConfig {
    val ignoreFile: String
    val production: Boolean
    val allowNewSignups: Boolean
    val fullScanCron: Cron
    val metadataRefreshCron: Cron
    val domain: String
    val TLS: Boolean
    val analyzerThreads: Int
    val port: Int
    val audibleRegion: AudibleRegions
    val database: DatabaseConnection
    val configDirectory: String
}

data class ThothConfigImpl(
    override val ignoreFile: String,
    override val production: Boolean,
    override val allowNewSignups: Boolean,
    override val fullScanCron: Cron,
    override val metadataRefreshCron: Cron,
    override val analyzerThreads: Int,
    override val port: Int,
    override val domain: String,
    override val TLS: Boolean,
    override val audibleRegion: AudibleRegions,
    override val database: DatabaseConnectionImpl,
    override val configDirectory: String,
) : ThothConfig
